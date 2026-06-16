package com.openlinkhub.digitalhuman.runtime.api.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.openlinkhub.digitalhuman.runtime.config.FunAsrProperties;
import com.openlinkhub.digitalhuman.runtime.orchestration.ConversationOrchestrator;
import com.openlinkhub.digitalhuman.runtime.rag.RagAnswerService;
import com.openlinkhub.digitalhuman.runtime.tts.TtsService;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.BinaryMessage;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.BinaryWebSocketHandler;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class RealtimeConversationWebSocketHandler extends BinaryWebSocketHandler {

    private final FunAsrProperties properties;
    private final RagAnswerService ragAnswerService;
    private final TtsService ttsService;
    private final ConversationOrchestrator conversationOrchestrator;
    private final ObjectMapper objectMapper;
    private final Map<String, RealtimeConversationSession> sessions = new ConcurrentHashMap<>();

    public RealtimeConversationWebSocketHandler(
            FunAsrProperties properties,
            RagAnswerService ragAnswerService,
            TtsService ttsService,
            ConversationOrchestrator conversationOrchestrator,
            ObjectMapper objectMapper
    ) {
        this.properties = properties;
        this.ragAnswerService = ragAnswerService;
        this.ttsService = ttsService;
        this.conversationOrchestrator = conversationOrchestrator;
        this.objectMapper = objectMapper;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        sessions.put(session.getId(), new RealtimeConversationSession(session, properties, ragAnswerService, ttsService, conversationOrchestrator, objectMapper));
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
        StreamingControlMessage controlMessage;
        try {
            controlMessage = StreamingControlMessage.parse(objectMapper, message.getPayload());
        } catch (IOException exception) {
            sendProtocolError(session, "Invalid control message: " + exception.getMessage());
            return;
        }
        RealtimeConversationSession streamingSession = sessions.get(session.getId());
        if (streamingSession == null) {
            return;
        }
        if (controlMessage.isStart()) {
            streamingSession.updateTtsEnabled(controlMessage.ttsEnabled());
            streamingSession.start(controlMessage.sampleRate());
        } else if (controlMessage.isStop()) {
            streamingSession.stop();
        } else if (controlMessage.isQuery()) {
            streamingSession.updateTtsEnabled(controlMessage.ttsEnabled());
            streamingSession.query(controlMessage.question());
        } else if (controlMessage.isConfig()) {
            streamingSession.updateTtsEnabled(controlMessage.ttsEnabled());
        }
    }

    @Override
    protected void handleBinaryMessage(WebSocketSession session, BinaryMessage message) {
        RealtimeConversationSession streamingSession = sessions.get(session.getId());
        if (streamingSession != null) {
            streamingSession.sendAudio(message);
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        RealtimeConversationSession streamingSession = sessions.remove(session.getId());
        if (streamingSession != null) {
            streamingSession.close();
        }
    }

    private void sendProtocolError(WebSocketSession session, String message) {
        if (!session.isOpen()) {
            return;
        }
        try {
            String payload = objectMapper.writeValueAsString(Map.of("type", "error", "message", message));
            session.sendMessage(new TextMessage(payload));
        } catch (IOException ignored) {
            // The connection is already unhealthy; the close callback will clean the server-side session.
        }
    }
}
