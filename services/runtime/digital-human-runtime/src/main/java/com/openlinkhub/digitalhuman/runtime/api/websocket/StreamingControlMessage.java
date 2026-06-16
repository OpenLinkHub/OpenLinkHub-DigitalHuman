package com.openlinkhub.digitalhuman.runtime.api.websocket;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;

record StreamingControlMessage(String type, Integer sampleRate, String question) {

    static StreamingControlMessage parse(ObjectMapper objectMapper, String payload) throws IOException {
        JsonNode root = objectMapper.readTree(payload);
        String type = root.path("type").asText("");
        Integer sampleRate = root.hasNonNull("sampleRate") ? root.path("sampleRate").asInt() : null;
        String question = root.hasNonNull("question") ? root.path("question").asText("") : null;
        return new StreamingControlMessage(type, sampleRate, question);
    }

    boolean isStart() {
        return "start".equalsIgnoreCase(type);
    }

    boolean isStop() {
        return "stop".equalsIgnoreCase(type);
    }

    boolean isQuery() {
        return "query".equalsIgnoreCase(type);
    }
}
