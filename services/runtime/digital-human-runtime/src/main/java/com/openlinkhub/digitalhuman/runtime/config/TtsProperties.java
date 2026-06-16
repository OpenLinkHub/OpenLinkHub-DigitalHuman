package com.openlinkhub.digitalhuman.runtime.config;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "dashscope.tts")
public record TtsProperties(
        boolean enabled,
        String apiKey,
        @NotBlank String model,
        @NotBlank String voice,
        @NotBlank String format,
        @Min(0) @Max(100) int volume,
        float speechRate,
        float pitchRate,
        @Min(1) long timeoutSeconds
) {
    public TtsProperties {
        if (model == null || model.isBlank()) {
            model = "cosyvoice-v2";
        }
        if (voice == null || voice.isBlank()) {
            voice = "longxiaochun_v2";
        }
        if (format == null || format.isBlank()) {
            format = "MP3_22050HZ_MONO_256KBPS";
        }
        if (volume <= 0) {
            volume = 50;
        }
        if (timeoutSeconds <= 0) {
            timeoutSeconds = 60;
        }
    }

    public boolean hasApiKey() {
        return apiKey != null && !apiKey.isBlank();
    }
}
