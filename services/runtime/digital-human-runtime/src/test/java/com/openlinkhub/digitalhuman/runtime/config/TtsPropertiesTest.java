package com.openlinkhub.digitalhuman.runtime.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.source.MapConfigurationPropertySource;

import java.util.Map;

class TtsPropertiesTest {

    @Test
    void bindsDashScopeTtsDefaultsAndOverrides() {
        var source = new MapConfigurationPropertySource(Map.of(
                "dashscope.tts.enabled", "true",
                "dashscope.tts.api-key", "test-key",
                "dashscope.tts.model", "cosyvoice-v2",
                "dashscope.tts.voice", "longxiaochun_v2",
                "dashscope.tts.format", "MP3_22050HZ_MONO_256KBPS",
                "dashscope.tts.volume", "60",
                "dashscope.tts.speech-rate", "1.1",
                "dashscope.tts.pitch-rate", "0.9"
        ));

        TtsProperties properties = new Binder(source)
                .bind("dashscope.tts", TtsProperties.class)
                .orElseThrow(() -> new AssertionError("dashscope.tts properties should bind"));

        assertThat(properties.enabled()).isTrue();
        assertThat(properties.apiKey()).isEqualTo("test-key");
        assertThat(properties.model()).isEqualTo("cosyvoice-v2");
        assertThat(properties.voice()).isEqualTo("longxiaochun_v2");
        assertThat(properties.format()).isEqualTo("MP3_22050HZ_MONO_256KBPS");
        assertThat(properties.volume()).isEqualTo(60);
        assertThat(properties.speechRate()).isEqualTo(1.1f);
        assertThat(properties.pitchRate()).isEqualTo(0.9f);
    }
}
