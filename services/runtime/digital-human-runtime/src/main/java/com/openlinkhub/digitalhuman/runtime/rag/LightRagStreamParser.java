package com.openlinkhub.digitalhuman.runtime.rag;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.util.Optional;

public class LightRagStreamParser {

    private final ObjectMapper objectMapper;

    public LightRagStreamParser(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public Optional<LightRagStreamChunk> parseLine(String line) throws IOException {
        if (line == null || line.isBlank()) {
            return Optional.empty();
        }
        JsonNode root = objectMapper.readTree(line);
        if (root.has("response")) {
            return Optional.of(new LightRagStreamChunk(root.path("response").asText("")));
        }
        return Optional.empty();
    }
}
