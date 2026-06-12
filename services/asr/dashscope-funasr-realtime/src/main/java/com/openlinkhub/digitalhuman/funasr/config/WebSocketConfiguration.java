package com.openlinkhub.digitalhuman.funasr.config;

import com.openlinkhub.digitalhuman.funasr.websocket.FunAsrStreamingWebSocketHandler;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
public class WebSocketConfiguration implements WebSocketConfigurer {

    private final FunAsrStreamingWebSocketHandler funAsrStreamingWebSocketHandler;

    public WebSocketConfiguration(FunAsrStreamingWebSocketHandler funAsrStreamingWebSocketHandler) {
        this.funAsrStreamingWebSocketHandler = funAsrStreamingWebSocketHandler;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(funAsrStreamingWebSocketHandler, "/ws/funasr/realtime")
                .setAllowedOriginPatterns("*");
    }
}
