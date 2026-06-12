package com.openlinkhub.digitalhuman.funasr.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.openlinkhub.digitalhuman.funasr.config.FunAsrProperties;
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
public class FunAsrStreamingWebSocketHandler extends BinaryWebSocketHandler {

    private final FunAsrProperties properties;
    private final ObjectMapper objectMapper;
    private final Map<String, FunAsrStreamingSession> sessions = new ConcurrentHashMap<>();

    public FunAsrStreamingWebSocketHandler(FunAsrProperties properties, ObjectMapper objectMapper) {
        this.properties = properties;
        this.objectMapper = objectMapper;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        sessions.put(session.getId(), new FunAsrStreamingSession(session, properties, objectMapper));
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
        FunAsrStreamingSession streamingSession = sessions.get(session.getId());
        if (streamingSession == null) {
            return;
        }
        if (controlMessage.isStart()) {
            streamingSession.start(controlMessage.sampleRate());
        } else if (controlMessage.isStop()) {
            streamingSession.stop();
        }
    }

    @Override
    protected void handleBinaryMessage(WebSocketSession session, BinaryMessage message) {
        FunAsrStreamingSession streamingSession = sessions.get(session.getId());
        if (streamingSession != null) {
            streamingSession.sendAudio(message);
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        FunAsrStreamingSession streamingSession = sessions.remove(session.getId());
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
