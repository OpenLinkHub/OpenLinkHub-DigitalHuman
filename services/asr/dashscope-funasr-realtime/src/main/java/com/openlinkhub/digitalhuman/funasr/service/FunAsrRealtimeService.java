package com.openlinkhub.digitalhuman.funasr.service;

import com.alibaba.dashscope.audio.asr.recognition.Recognition;
import com.alibaba.dashscope.audio.asr.recognition.RecognitionParam;
import com.alibaba.dashscope.audio.asr.recognition.RecognitionResult;
import com.alibaba.dashscope.common.ResultCallback;
import com.openlinkhub.digitalhuman.funasr.config.FunAsrProperties;
import com.openlinkhub.digitalhuman.funasr.dto.FunAsrRecognitionEvent;
import com.openlinkhub.digitalhuman.funasr.dto.FunAsrRecognitionResponse;
import com.openlinkhub.digitalhuman.funasr.exception.FunAsrException;
import org.springframework.stereotype.Service;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

@Service
public class FunAsrRealtimeService {

    private final FunAsrProperties properties;
    private final FfmpegAudioTranscoder audioTranscoder;

    public FunAsrRealtimeService(FunAsrProperties properties, FfmpegAudioTranscoder audioTranscoder) {
        this.properties = properties;
        this.audioTranscoder = audioTranscoder;
    }

    public FunAsrRecognitionResponse recognize(
            byte[] audio,
            String filename,
            String contentType,
            String formatOverride,
            Integer sampleRateOverride
    ) {
        if (!properties.hasApiKey()) {
            throw new FunAsrException("DashScope API key is missing. Set DASHSCOPE_API_KEY or dashscope.funasr.api-key.");
        }
        if (audio == null || audio.length == 0) {
            throw new FunAsrException("Audio file is empty.");
        }

        PreparedAudio preparedAudio = prepareAudio(audio, filename, contentType, formatOverride, sampleRateOverride);
        List<ByteBuffer> frames = AudioFrameChunker.chunk(preparedAudio.audio(), properties.chunkSizeBytes());
        Recognition recognition = new Recognition();
        RecognitionCollector callback = new RecognitionCollector();

        recognition.call(buildParam(preparedAudio), callback);
        try {
            for (ByteBuffer frame : frames) {
                recognition.sendAudioFrame(frame);
                sleepFrameInterval();
            }
            recognition.stop();
            callback.await(properties.callbackTimeoutSeconds());
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new FunAsrException("Interrupted while waiting for FunASR realtime recognition.", exception);
        } catch (RuntimeException exception) {
            throw new FunAsrException("FunASR realtime recognition failed: " + exception.getMessage(), exception);
        }

        if (callback.error() != null) {
            throw new FunAsrException("FunASR callback failed: " + callback.error().getMessage(), callback.error());
        }

        return new FunAsrRecognitionResponse(
                firstNonBlank(recognition.getLastRequestId(), callback.lastRequestId()),
                callback.combinedText(),
                callback.events(),
                recognition.getFirstPackageDelay(),
                recognition.getLastPackageDelay(),
                preparedAudio.audio().length,
                frames.size()
        );
    }

    private PreparedAudio prepareAudio(
            byte[] audio,
            String filename,
            String contentType,
            String formatOverride,
            Integer sampleRateOverride
    ) {
        if (AudioFormatResolver.shouldTranscodeToPcm(formatOverride, filename, contentType)) {
            return new PreparedAudio(audioTranscoder.toPcm16kMono(audio), "pcm", 16000);
        }
        String format = AudioFormatResolver.resolve(formatOverride, filename, contentType, properties.format());
        int sampleRate = sampleRateOverride != null ? sampleRateOverride : properties.sampleRate();
        return new PreparedAudio(audio, format, sampleRate);
    }

    private RecognitionParam buildParam(PreparedAudio preparedAudio) {
        RecognitionParam.RecognitionParamBuilder<?, ?> builder = RecognitionParam.builder()
                .apiKey(properties.apiKey())
                .model(properties.model())
                .format(preparedAudio.format())
                .sampleRate(preparedAudio.sampleRate())
                .disfluencyRemovalEnabled(properties.disfluencyRemovalEnabled());

        if (properties.phraseId() != null && !properties.phraseId().isBlank()) {
            builder.phraseId(properties.phraseId());
        }
        if (properties.vocabularyId() != null && !properties.vocabularyId().isBlank()) {
            builder.vocabularyId(properties.vocabularyId());
        }
        return builder.build();
    }

    private record PreparedAudio(byte[] audio, String format, int sampleRate) {
    }

    private void sleepFrameInterval() throws InterruptedException {
        if (properties.frameIntervalMs() > 0) {
            Thread.sleep(properties.frameIntervalMs());
        }
    }

    private String firstNonBlank(String first, String second) {
        if (first != null && !first.isBlank()) {
            return first;
        }
        return second;
    }

    private static final class RecognitionCollector extends ResultCallback<RecognitionResult> {

        private final CountDownLatch completed = new CountDownLatch(1);
        private final AtomicReference<Exception> error = new AtomicReference<>();
        private final List<FunAsrRecognitionEvent> events = new ArrayList<>();

        @Override
        public void onEvent(RecognitionResult result) {
            if (result == null || result.getSentence() == null) {
                return;
            }
            events.add(new FunAsrRecognitionEvent(
                    result.getRequestId(),
                    result.getSentence().getText(),
                    result.getSentence().getBeginTime(),
                    result.getSentence().getEndTime(),
                    result.isSentenceBegin(),
                    result.isSentenceEnd(),
                    result.isCompleteResult()
            ));
        }

        @Override
        public void onComplete() {
            completed.countDown();
        }

        @Override
        public void onError(Exception exception) {
            error.set(exception);
            completed.countDown();
        }

        void await(long timeoutSeconds) throws InterruptedException {
            boolean finished = completed.await(timeoutSeconds, TimeUnit.SECONDS);
            if (!finished) {
                throw new FunAsrException("Timed out waiting for FunASR realtime recognition callback.");
            }
        }

        Exception error() {
            return error.get();
        }

        List<FunAsrRecognitionEvent> events() {
            return List.copyOf(events);
        }

        String combinedText() {
            return events.stream()
                    .map(FunAsrRecognitionEvent::text)
                    .filter(Objects::nonNull)
                    .filter(text -> !text.isBlank())
                    .reduce((previous, current) -> current)
                    .orElse("");
        }

        String lastRequestId() {
            for (int index = events.size() - 1; index >= 0; index--) {
                String requestId = events.get(index).requestId();
                if (requestId != null && !requestId.isBlank()) {
                    return requestId;
                }
            }
            return null;
        }
    }
}
