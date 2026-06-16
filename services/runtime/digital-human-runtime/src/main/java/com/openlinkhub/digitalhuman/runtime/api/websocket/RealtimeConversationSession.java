package com.openlinkhub.digitalhuman.runtime.api.websocket;

import com.alibaba.dashscope.audio.asr.recognition.Recognition;
import com.alibaba.dashscope.audio.asr.recognition.RecognitionParam;
import com.alibaba.dashscope.audio.asr.recognition.RecognitionResult;
import com.alibaba.dashscope.common.ResultCallback;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.openlinkhub.digitalhuman.runtime.config.FunAsrProperties;
import com.openlinkhub.digitalhuman.runtime.common.exception.FunAsrException;
import com.openlinkhub.digitalhuman.runtime.orchestration.ConversationOrchestrator;
import com.openlinkhub.digitalhuman.runtime.orchestration.ConversationResponseSink;
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
import java.util.concurrent.atomic.AtomicLong;

public class RealtimeConversationSession {

    private final WebSocketSession webSocketSession;
    private final FunAsrProperties properties;
    private final RagAnswerService ragAnswerService;
    private final TtsService ttsService;
    private final ConversationOrchestrator conversationOrchestrator;
    private final ObjectMapper objectMapper;
    private final Object sendLock = new Object();
    private final ExecutorService answerExecutor = Executors.newSingleThreadExecutor();
    private final AtomicBoolean started = new AtomicBoolean(false);
    private final AtomicBoolean stopped = new AtomicBoolean(false);
    private final AtomicBoolean answering = new AtomicBoolean(false);
    private final AtomicBoolean ttsEnabled = new AtomicBoolean(true);
    private final AtomicLong ttsGeneration = new AtomicLong(0);

    private Recognition recognition;

    public RealtimeConversationSession(
            WebSocketSession webSocketSession,
            FunAsrProperties properties,
            RagAnswerService ragAnswerService,
            TtsService ttsService,
            ConversationOrchestrator conversationOrchestrator,
            ObjectMapper objectMapper
    ) {
        this.webSocketSession = webSocketSession;
        this.properties = properties;
        this.ragAnswerService = ragAnswerService;
        this.ttsService = ttsService;
        this.conversationOrchestrator = conversationOrchestrator;
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
        sendJson(payload(
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
        if (question == null || question.trim().isEmpty()) {
            sendError("Question is empty.");
            return;
        }
        handleRecognizedText(question.trim());
    }

    public void updateTtsEnabled(Boolean enabled) {
        if (enabled == null) {
            return;
        }
        ttsEnabled.set(enabled.booleanValue());
        Map<String, Object> payload = new LinkedHashMap<String, Object>();
        payload.put("type", "tts_config");
        payload.put("enabled", enabled.booleanValue());
        sendJson(payload);
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

        if (properties.phraseId() != null && !properties.phraseId().trim().isEmpty()) {
            builder.phraseId(properties.phraseId());
        }
        if (properties.vocabularyId() != null && !properties.vocabularyId().trim().isEmpty()) {
            builder.vocabularyId(properties.vocabularyId());
        }
        return builder.build();
    }

    private void sendStatus(String status) {
        sendJson(payload("type", "status", "status", status));
    }

    private void sendError(String message) {
        sendJson(payload("type", "error", "message", message));
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
            if (result.isSentenceEnd() && result.getSentence().getText() != null && !result.getSentence().getText().trim().isEmpty()) {
                handleRecognizedText(result.getSentence().getText());
            }
        }

        @Override
        public void onComplete() {
            sendJson(payload("type", "completed"));
        }

        @Override
        public void onError(Exception exception) {
            sendError(exception.getMessage());
        }
    }

    private void queryRagAnswer(String question) {
        if (!answering.compareAndSet(false, true)) {
            sendJson(payload("type", "answer_skipped", "reason", "answer_in_progress"));
            return;
        }
        answerExecutor.submit(() -> ragAnswerService.streamAnswer(question, new WebSocketRagAnswerSink()));
    }

    private void handleRecognizedText(String text) {
        if (text == null || text.trim().isEmpty()) {
            return;
        }
        ttsGeneration.incrementAndGet();
        conversationOrchestrator.handle(text.trim(), new WebSocketConversationResponseSink());
    }

    private final class WebSocketConversationResponseSink implements ConversationResponseSink {

        @Override
        public void sendEvent(Map<String, ?> event) {
            if ("tts_stop".equals(event.get("type"))) {
                ttsGeneration.incrementAndGet();
            }
            sendJson(event);
        }

        @Override
        public void queryRag(String question) {
            queryRagAnswer(question);
        }

        @Override
        public void speak(String text) {
            answerExecutor.submit(() -> synthesizePlainText(text));
        }
    }

    private final class WebSocketRagAnswerSink implements RagAnswerSink {

        @Override
        public void onStart(String question) {
            sendJson(payload("type", "answer_started", "question", question));
        }

        @Override
        public void onDelta(String delta) {
            sendJson(payload("type", "answer_delta", "text", delta));
        }

        @Override
        public void onCompleted(String answer) {
            sendJson(payload("type", "answer_completed", "text", answer));
            answering.set(false);
            synthesizeAnswer(answer);
        }

        @Override
        public void onError(String message) {
            sendJson(payload("type", "answer_error", "message", message));
            answering.set(false);
        }
    }

    private void synthesizeAnswer(String answer) {
        if (!ttsService.isEnabled()) {
            return;
        }
        AnswerTextExtractor.ExtractedAnswer extractedAnswer = AnswerTextExtractor.extract(answer);
        String speechText = extractedAnswer.speechText();
        synthesizePlainText(speechText);
    }

    private void synthesizePlainText(String speechText) {
        if (!ttsEnabled.get() || !ttsService.isEnabled()) {
            return;
        }
        long generation = ttsGeneration.get();
        if (speechText == null || speechText.trim().isEmpty()) {
            sendJson(payload("type", "tts_skipped", "reason", "empty_speech_text"));
            return;
        }
        try {
            sendJson(payload("type", "tts_started", "text", speechText));
            TtsAudio audio = ttsService.synthesize(speechText);
            if (generation != ttsGeneration.get()) {
                return;
            }
            sendJson(payload(
                    "type", "tts_audio",
                    "mimeType", audio.mimeType(),
                    "format", audio.format(),
                    "audio", Base64.getEncoder().encodeToString(audio.bytes())
            ));
        } catch (RuntimeException exception) {
            sendJson(payload(
                    "type", "tts_error",
                    "message", exception.getMessage()
            ));
        }
    }

    private Map<String, Object> payload(Object... keysAndValues) {
        Map<String, Object> payload = new LinkedHashMap<String, Object>();
        for (int index = 0; index + 1 < keysAndValues.length; index += 2) {
            payload.put(String.valueOf(keysAndValues[index]), keysAndValues[index + 1]);
        }
        return payload;
    }
}
