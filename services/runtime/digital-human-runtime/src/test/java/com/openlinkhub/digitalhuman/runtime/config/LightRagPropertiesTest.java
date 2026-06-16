package com.openlinkhub.digitalhuman.runtime.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.source.MapConfigurationPropertySource;

import java.util.Map;

class LightRagPropertiesTest {

    @Test
    void bindsLightRagDefaultsAndBuildsStreamUrl() {
        var source = new MapConfigurationPropertySource(Map.of(
                "lightrag.base-url", "http://127.0.0.1:9621/",
                "lightrag.stream-path", "query/stream",
                "lightrag.query-mode", "mix"
        ));

        LightRagProperties properties = new Binder(source)
                .bind("lightrag", LightRagProperties.class)
                .orElseThrow(() -> new AssertionError("lightrag properties should bind"));

        assertThat(properties.streamUrl()).isEqualTo("http://127.0.0.1:9621/query/stream");
        assertThat(properties.includeReferences()).isTrue();
        assertThat(properties.responseType()).isEqualTo("Multiple Paragraphs");
    }
}
