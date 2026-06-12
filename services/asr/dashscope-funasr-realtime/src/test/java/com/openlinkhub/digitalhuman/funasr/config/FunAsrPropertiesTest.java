package com.openlinkhub.digitalhuman.funasr.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.source.MapConfigurationPropertySource;

import java.util.Map;

class FunAsrPropertiesTest {

    @Test
    void bindsDashScopeFunAsrDefaultsAndOverrides() {
        var source = new MapConfigurationPropertySource(Map.of(
                "dashscope.funasr.api-key", "test-key",
                "dashscope.funasr.model", "fun-asr-realtime",
                "dashscope.funasr.format", "pcm",
                "dashscope.funasr.sample-rate", "16000",
                "dashscope.funasr.chunk-size-bytes", "3200",
                "dashscope.funasr.disfluency-removal-enabled", "true"
        ));

        FunAsrProperties properties = new Binder(source)
                .bind("dashscope.funasr", FunAsrProperties.class)
                .orElseThrow(() -> new AssertionError("dashscope.funasr properties should bind"));

        assertThat(properties.apiKey()).isEqualTo("test-key");
        assertThat(properties.model()).isEqualTo("fun-asr-realtime");
        assertThat(properties.format()).isEqualTo("pcm");
        assertThat(properties.sampleRate()).isEqualTo(16000);
        assertThat(properties.chunkSizeBytes()).isEqualTo(3200);
        assertThat(properties.disfluencyRemovalEnabled()).isTrue();
    }
}
