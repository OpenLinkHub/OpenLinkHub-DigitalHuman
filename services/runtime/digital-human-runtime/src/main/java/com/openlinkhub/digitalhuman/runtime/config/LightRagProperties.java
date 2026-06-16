package com.openlinkhub.digitalhuman.runtime.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "lightrag")
public record LightRagProperties(
        String baseUrl,
        String streamPath,
        String queryMode,
        String responseType,
        Boolean includeReferences,
        String bearerToken,
        String username,
        String password,
        String apiKeyHeaderValue,
        Duration connectTimeout,
        Duration requestTimeout
) {
    public LightRagProperties {
        if (baseUrl == null || baseUrl.isBlank()) {
            baseUrl = "http://127.0.0.1:9621";
        }
        if (streamPath == null || streamPath.isBlank()) {
            streamPath = "/query/stream";
        }
        if (queryMode == null || queryMode.isBlank()) {
            queryMode = "mix";
        }
        if (responseType == null || responseType.isBlank()) {
            responseType = "Multiple Paragraphs";
        }
        if (includeReferences == null) {
            includeReferences = true;
        }
        if (connectTimeout == null) {
            connectTimeout = Duration.ofSeconds(5);
        }
        if (requestTimeout == null) {
            requestTimeout = Duration.ofSeconds(120);
        }
    }

    public String streamUrl() {
        String normalizedBaseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        String normalizedPath = streamPath.startsWith("/") ? streamPath : "/" + streamPath;
        String url = normalizedBaseUrl + normalizedPath;
        if (apiKeyHeaderValue != null && !apiKeyHeaderValue.isBlank()) {
            return url + "?api_key_header_value=" + apiKeyHeaderValue;
        }
        return url;
    }

    public String loginUrl() {
        String normalizedBaseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        return normalizedBaseUrl + "/login";
    }

    public boolean hasBearerToken() {
        return bearerToken != null && !bearerToken.isBlank();
    }

    public boolean hasLoginCredentials() {
        return username != null && !username.isBlank() && password != null && !password.isBlank();
    }
}
