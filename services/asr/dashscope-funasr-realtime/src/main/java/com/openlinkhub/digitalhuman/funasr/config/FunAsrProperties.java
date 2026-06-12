package com.openlinkhub.digitalhuman.funasr.config;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "dashscope.funasr")
public record FunAsrProperties(
        String apiKey,
        @NotBlank String model,
        @NotBlank String format,
        @Min(1) Integer sampleRate,
        @Min(1) int chunkSizeBytes,
        @Min(0) long frameIntervalMs,
        @Min(1) long callbackTimeoutSeconds,
        boolean disfluencyRemovalEnabled,
        String phraseId,
        String vocabularyId
) {
    public FunAsrProperties {
        if (model == null || model.isBlank()) {
            model = "fun-asr-realtime";
        }
        if (format == null || format.isBlank()) {
            format = "pcm";
        }
        if (sampleRate == null) {
            sampleRate = 16000;
        }
        if (chunkSizeBytes <= 0) {
            chunkSizeBytes = 3200;
        }
        if (callbackTimeoutSeconds <= 0) {
            callbackTimeoutSeconds = 60;
        }
    }

    public boolean hasApiKey() {
        return apiKey != null && !apiKey.isBlank();
    }
}
