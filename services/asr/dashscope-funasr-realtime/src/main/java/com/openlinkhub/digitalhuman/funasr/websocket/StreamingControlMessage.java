package com.openlinkhub.digitalhuman.funasr.websocket;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;

record StreamingControlMessage(String type, Integer sampleRate) {

    static StreamingControlMessage parse(ObjectMapper objectMapper, String payload) throws IOException {
        JsonNode root = objectMapper.readTree(payload);
        String type = root.path("type").asText("");
        Integer sampleRate = root.hasNonNull("sampleRate") ? root.path("sampleRate").asInt() : null;
        return new StreamingControlMessage(type, sampleRate);
    }

    boolean isStart() {
        return "start".equalsIgnoreCase(type);
    }

    boolean isStop() {
        return "stop".equalsIgnoreCase(type);
    }
}
