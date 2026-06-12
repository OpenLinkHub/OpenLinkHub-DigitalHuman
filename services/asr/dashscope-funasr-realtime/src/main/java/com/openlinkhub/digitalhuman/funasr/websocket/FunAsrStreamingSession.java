package com.openlinkhub.digitalhuman.funasr.websocket;

import com.alibaba.dashscope.audio.asr.recognition.Recognition;
import com.alibaba.dashscope.audio.asr.recognition.RecognitionParam;
import com.alibaba.dashscope.audio.asr.recognition.RecognitionResult;
import com.alibaba.dashscope.common.ResultCallback;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.openlinkhub.digitalhuman.funasr.config.FunAsrProperties;
import com.openlinkhub.digitalhuman.funasr.exception.FunAsrException;
import org.springframework.web.socket.BinaryMessage;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

public class FunAsrStreamingSession {

    private final WebSocketSession webSocketSession;
    private final FunAsrProperties properties;
    private final ObjectMapper objectMapper;
    private final Object sendLock = new Object();
    private final AtomicBoolean started = new AtomicBoolean(false);
    private final AtomicBoolean stopped = new AtomicBoolean(false);

    private Recognition recognition;

    public FunAsrStreamingSession(
            WebSocketSession webSocketSession,
            FunAsrProperties properties,
            ObjectMapper objectMapper
    ) {
        this.webSocketSession = webSocketSession;
        this.properties = properties;
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

    public void close() {
        if (started.get() && stopped.compareAndSet(false, true)) {
            recognition.stop();
        }
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
}
