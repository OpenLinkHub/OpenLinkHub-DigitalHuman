package com.openlinkhub.digitalhuman.runtime.rag;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

class LightRagStreamParserTest {

    private final LightRagStreamParser parser = new LightRagStreamParser(new ObjectMapper());

    @Test
    void parsesResponseDeltaFromNdjsonLine() throws Exception {
        var chunk = parser.parseLine("{\"response\":\"你好\"}");

        assertThat(chunk).isPresent();
        assertThat(chunk.orElseThrow().response()).isEqualTo("你好");
    }

    @Test
    void ignoresReferenceOnlyLines() throws Exception {
        var chunk = parser.parseLine("{\"references\":[{\"file_path\":\"demo.md\"}]}");

        assertThat(chunk).isEmpty();
    }
}
