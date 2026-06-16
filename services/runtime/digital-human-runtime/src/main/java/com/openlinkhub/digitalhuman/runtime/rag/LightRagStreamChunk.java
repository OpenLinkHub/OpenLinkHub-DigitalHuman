package com.openlinkhub.digitalhuman.runtime.rag;

public record LightRagStreamChunk(String response) {

    public boolean hasResponse() {
        return response != null && !response.isEmpty();
    }
}
