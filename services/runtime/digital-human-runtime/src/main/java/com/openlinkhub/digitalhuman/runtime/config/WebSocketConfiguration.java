package com.openlinkhub.digitalhuman.runtime.config;

import com.openlinkhub.digitalhuman.runtime.api.websocket.RealtimeConversationWebSocketHandler;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
public class WebSocketConfiguration implements WebSocketConfigurer {

    private final RealtimeConversationWebSocketHandler realtimeConversationWebSocketHandler;

    public WebSocketConfiguration(RealtimeConversationWebSocketHandler realtimeConversationWebSocketHandler) {
        this.realtimeConversationWebSocketHandler = realtimeConversationWebSocketHandler;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(realtimeConversationWebSocketHandler, "/ws/runtime/conversation")
                .setAllowedOriginPatterns("*");
    }
}
