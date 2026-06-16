package com.openlinkhub.digitalhuman.runtime.api.websocket;

import com.alibaba.dashscope.audio.asr.recognition.Recognition;
import com.alibaba.dashscope.audio.asr.recognition.RecognitionParam;
import com.alibaba.dashscope.audio.asr.recognition.RecognitionResult;
import com.alibaba.dashscope.common.ResultCallback;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.openlinkhub.digitalhuman.runtime.config.FunAsrProperties;
import com.openlinkhub.digitalhuman.runtime.common.exception.FunAsrException;
import com.openlinkhub.digitalhuman.runtime.rag.RagAnswerService;
import com.openlinkhub.digitalhuman.runtime.rag.RagAnswerSink;
import com.openlinkhub.digitalhuman.runtime.tts.AnswerTextExtractor;
import com.openlinkhub.digitalhuman.runtime.tts.TtsAudio;
import com.openlinkhub.digitalhuman.runtime.tts.TtsService;
import org.springframework.web.socket.BinaryMessage;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

public class RealtimeConversationSession {

    private final WebSocketSession webSocketSession;
    private final FunAsrProperties properties;
    private final RagAnswerService ragAnswerService;
    private final TtsService ttsService;
    private final ObjectMapper objectMapper;
    private final Object sendLock = new Object();
    private final ExecutorService answerExecutor = Executors.newSingleThreadExecutor();
    private final AtomicBoolean started = new AtomicBoolean(false);
    private final AtomicBoolean stopped = new AtomicBoolean(false);
    private final AtomicBoolean answering = new AtomicBoolean(false);

    private Recognition recognition;

    public RealtimeConversationSession(
            WebSocketSession webSocketSession,
            FunAsrProperties properties,
            RagAnswerService ragAnswerService,
            TtsService ttsService,
            ObjectMapper objectMapper
    ) {
        this.webSocketSession = webSocketSession;
        this.properties = properties;
        this.ragAnswerService = ragAnswerService;
        this.ttsService = ttsService;
        this.objectMapper = objectMapper;
    }

    public void start(Integer sampleRate) {
        if (!properties.hasApiKey()) {
            throw new FunAsrException("DashScope API key is missing. Set DASHSCOPE_API_KEY or dashscope.funasr.api-key.");
        }
        if (!started.compareAndSet(false, true)) {
            sendStatus("already_started");
            return;
        }

        int effectiveSampleRate = sampleRate != null && sampleRate > 0 ? sampleRate : properties.sampleRate();
        recognition = new Recognition();
        recognition.call(buildParam(effectiveSampleRate), new StreamingCallback());
        sendJson(Map.of(
                "type", "started",
                "sampleRate", effectiveSampleRate,
                "format", "pcm"
        ));
    }

    public void sendAudio(BinaryMessage message) {
        if (!started.get() || stopped.get()) {
            return;
        }
        ByteBuffer payload = message.getPayload();
        recognition.sendAudioFrame(payload);
    }

    public void stop() {
        if (!started.get() || !stopped.compareAndSet(false, true)) {
            return;
        }
        recognition.stop();
        sendStatus("stopping");
    }

    public void query(String question) {
        if (question == null || question.isBlank()) {
            sendError("Question is empty.");
            return;
        }
        queryRagAnswer(question.trim());
    }

    public void close() {
        if (started.get() && stopped.compareAndSet(false, true)) {
            recognition.stop();
        }
        answerExecutor.shutdownNow();
    }

    private RecognitionParam buildParam(int sampleRate) {
        RecognitionParam.RecognitionParamBuilder<?, ?> builder = RecognitionParam.builder()
                .apiKey(properties.apiKey())
                .model(properties.model())
                .format("pcm")
                .sampleRate(sampleRate)
                .disfluencyRemovalEnabled(properties.disfluencyRemovalEnabled());

        if (properties.phraseId() != null && !properties.phraseId().isBlank()) {
            builder.phraseId(properties.phraseId());
        }
        if (properties.vocabularyId() != null && !properties.vocabularyId().isBlank()) {
            builder.vocabularyId(properties.vocabularyId());
        }
        return builder.build();
    }

    private void sendStatus(String status) {
        sendJson(Map.of("type", "status", "status", status));
    }

    private void sendError(String message) {
        sendJson(Map.of("type", "error", "message", message));
    }

    private void sendJson(Map<String, ?> payload) {
        if (!webSocketSession.isOpen()) {
            return;
        }
        try {
            String json = objectMapper.writeValueAsString(payload);
            synchronized (sendLock) {
                if (webSocketSession.isOpen()) {
                    webSocketSession.sendMessage(new TextMessage(json));
                }
            }
        } catch (JsonProcessingException exception) {
            throw new FunAsrException("Failed to serialize websocket message.", exception);
        } catch (IOException exception) {
            throw new FunAsrException("Failed to send websocket message.", exception);
        }
    }

    private final class StreamingCallback extends ResultCallback<RecognitionResult> {

        @Override
        public void onEvent(RecognitionResult result) {
            if (result == null || result.getSentence() == null) {
                return;
            }
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("type", "recognition");
            payload.put("requestId", result.getRequestId());
            payload.put("text", result.getSentence().getText());
            payload.put("beginTime", result.getSentence().getBeginTime());
            payload.put("endTime", result.getSentence().getEndTime());
            payload.put("sentenceBegin", result.isSentenceBegin());
            payload.put("sentenceEnd", result.isSentenceEnd());
            payload.put("completeResult", result.isCompleteResult());
            sendJson(payload);
            if (result.isSentenceEnd() && result.getSentence().getText() != null && !result.getSentence().getText().isBlank()) {
                queryRagAnswer(result.getSentence().getText());
            }
        }

        @Override
        public void onComplete() {
            sendJson(Map.of("type", "completed"));
        }

        @Override
        public void onError(Exception exception) {
            sendError(exception.getMessage());
        }
    }

    private void queryRagAnswer(String question) {
        if (!answering.compareAndSet(false, true)) {
            sendJson(Map.of("type", "answer_skipped", "reason", "answer_in_progress"));
            return;
        }
        answerExecutor.submit(() -> ragAnswerService.streamAnswer(question, new WebSocketRagAnswerSink()));
    }

    private final class WebSocketRagAnswerSink implements RagAnswerSink {

        @Override
        public void onStart(String question) {
            sendJson(Map.of("type", "answer_started", "question", question));
        }

        @Override
        public void onDelta(String delta) {
            sendJson(Map.of("type", "answer_delta", "text", delta));
        }

        @Override
        public void onCompleted(String answer) {
            sendJson(Map.of("type", "answer_completed", "text", answer));
            answering.set(false);
            synthesizeAnswer(answer);
        }

        @Override
        public void onError(String message) {
            sendJson(Map.of("type", "answer_error", "message", message));
            answering.set(false);
        }
    }

    private void synthesizeAnswer(String answer) {
        if (!ttsService.isEnabled()) {
            return;
        }
        AnswerTextExtractor.ExtractedAnswer extractedAnswer = AnswerTextExtractor.extract(answer);
        String speechText = extractedAnswer.speechText();
        if (speechText == null || speechText.isBlank()) {
            sendJson(Map.of("type", "tts_skipped", "reason", "empty_speech_text"));
            return;
        }
        try {
            sendJson(Map.of("type", "tts_started", "text", speechText));
            TtsAudio audio = ttsService.synthesize(speechText);
            sendJson(Map.of(
                    "type", "tts_audio",
                    "mimeType", audio.mimeType(),
                    "format", audio.format(),
                    "audio", Base64.getEncoder().encodeToString(audio.bytes())
            ));
        } catch (RuntimeException exception) {
            sendJson(Map.of(
                    "type", "tts_error",
                    "message", exception.getMessage()
            ));
        }
    }
}
