package com.openlinkhub.digitalhuman.runtime.api.websocket;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;

final class StreamingControlMessage {

    private final String type;
    private final Integer sampleRate;
    private final String question;
    private final Boolean ttsEnabled;

    private StreamingControlMessage(String type, Integer sampleRate, String question, Boolean ttsEnabled) {
        this.type = type;
        this.sampleRate = sampleRate;
        this.question = question;
        this.ttsEnabled = ttsEnabled;
    }

    static StreamingControlMessage parse(ObjectMapper objectMapper, String payload) throws IOException {
        JsonNode root = objectMapper.readTree(payload);
        String type = root.path("type").asText("");
        Integer sampleRate = root.hasNonNull("sampleRate") ? Integer.valueOf(root.path("sampleRate").asInt()) : null;
        String question = root.hasNonNull("question") ? root.path("question").asText("") : null;
        Boolean ttsEnabled = root.hasNonNull("ttsEnabled") ? Boolean.valueOf(root.path("ttsEnabled").asBoolean()) : null;
        return new StreamingControlMessage(type, sampleRate, question, ttsEnabled);
    }

    String type() {
        return type;
    }

    Integer sampleRate() {
        return sampleRate;
    }

    String question() {
        return question;
    }

    Boolean ttsEnabled() {
        return ttsEnabled;
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

    boolean isConfig() {
        return "config".equalsIgnoreCase(type);
    }
}
