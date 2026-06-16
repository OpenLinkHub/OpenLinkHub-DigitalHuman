package com.openlinkhub.digitalhuman.runtime.rag;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.openlinkhub.digitalhuman.runtime.config.LightRagProperties;
import com.openlinkhub.digitalhuman.runtime.common.exception.FunAsrException;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;

@Service
public class LightRagAnswerService implements RagAnswerService {

    private final LightRagProperties properties;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;
    private final LightRagStreamParser streamParser;
    private final AtomicReference<String> bearerToken = new AtomicReference<>();

    public LightRagAnswerService(LightRagProperties properties, ObjectMapper objectMapper) {
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(properties.connectTimeout())
                .build();
        this.streamParser = new LightRagStreamParser(objectMapper);
    }

    @Override
    public void streamAnswer(String question, RagAnswerSink sink) {
        if (question == null || question.isBlank()) {
            return;
        }

        sink.onStart(question);
        StringBuilder answer = new StringBuilder();
        try {
            String requestBody = buildRequestBody(question);
            HttpRequest request = buildQueryRequest(requestBody);

            HttpResponse<Stream<String>> response = httpClient.send(request, HttpResponse.BodyHandlers.ofLines());
            if (response.statusCode() == 401 && properties.hasLoginCredentials()) {
                bearerToken.set(login());
                request = buildQueryRequest(requestBody);
                response = httpClient.send(request, HttpResponse.BodyHandlers.ofLines());
            }
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new FunAsrException("LightRAG query failed with HTTP status " + response.statusCode());
            }

            try (Stream<String> lines = response.body()) {
                lines.forEach(line -> handleLine(line, answer, sink));
            }
            sink.onCompleted(answer.toString());
        } catch (IOException exception) {
            sink.onError("LightRAG query failed: " + exception.getMessage());
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            sink.onError("LightRAG query was interrupted.");
        } catch (RuntimeException exception) {
            sink.onError("LightRAG query failed: " + exception.getMessage());
        }
    }

    private String buildRequestBody(String question) throws JsonProcessingException {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("query", question);
        payload.put("mode", properties.queryMode());
        payload.put("stream", true);
        payload.put("include_references", properties.includeReferences());
        payload.put("response_type", properties.responseType());
        return objectMapper.writeValueAsString(payload);
    }

    private HttpRequest buildQueryRequest(String requestBody) {
        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create(properties.streamUrl()))
                .timeout(properties.requestTimeout())
                .header("Content-Type", "application/json")
                .header("Accept", "application/x-ndjson, application/json");
        String token = currentBearerToken();
        if (token != null && !token.isBlank()) {
            builder.header("Authorization", "Bearer " + token);
        }
        return builder.POST(HttpRequest.BodyPublishers.ofString(requestBody)).build();
    }

    private String currentBearerToken() {
        if (properties.hasBearerToken()) {
            return properties.bearerToken();
        }
        return bearerToken.get();
    }

    private String login() throws IOException, InterruptedException {
        String form = "username=" + encode(properties.username())
                + "&password=" + encode(properties.password());
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(properties.loginUrl()))
                .timeout(properties.requestTimeout())
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(form))
                .build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new FunAsrException("LightRAG login failed with HTTP status " + response.statusCode());
        }
        String token = objectMapper.readTree(response.body()).path("access_token").asText("");
        if (token.isBlank()) {
            throw new FunAsrException("LightRAG login response did not contain access_token.");
        }
        return token;
    }

    private String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    private void handleLine(String line, StringBuilder answer, RagAnswerSink sink) {
        try {
            streamParser.parseLine(line)
                    .filter(LightRagStreamChunk::hasResponse)
                    .ifPresent(chunk -> {
                        answer.append(chunk.response());
                        sink.onDelta(chunk.response());
                    });
        } catch (IOException exception) {
            throw new FunAsrException("Failed to parse LightRAG stream line.", exception);
        }
    }
}
