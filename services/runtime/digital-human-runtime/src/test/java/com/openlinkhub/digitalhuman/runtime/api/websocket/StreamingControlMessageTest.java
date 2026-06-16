package com.openlinkhub.digitalhuman.runtime.api.websocket;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

class StreamingControlMessageTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void parsesStartMessageWithSampleRate() throws Exception {
        StreamingControlMessage message = StreamingControlMessage.parse(
                objectMapper,
                "{\"type\":\"start\",\"sampleRate\":16000,\"ttsEnabled\":false}"
        );

        assertThat(message.isStart()).isTrue();
        assertThat(message.isStop()).isFalse();
        assertThat(message.sampleRate()).isEqualTo(16000);
        assertThat(message.ttsEnabled()).isFalse();
        assertThat(message.question()).isNull();
    }

    @Test
    void parsesStopMessageWithoutSampleRate() throws Exception {
        StreamingControlMessage message = StreamingControlMessage.parse(objectMapper, "{\"type\":\"stop\"}");

        assertThat(message.isStop()).isTrue();
        assertThat(message.sampleRate()).isNull();
    }

    @Test
    void parsesQueryMessage() throws Exception {
        StreamingControlMessage message = StreamingControlMessage.parse(
                objectMapper,
                "{\"type\":\"query\",\"question\":\"什么是 OpenLinkHub？\"}"
        );

        assertThat(message.isQuery()).isTrue();
        assertThat(message.question()).isEqualTo("什么是 OpenLinkHub？");
        assertThat(message.ttsEnabled()).isNull();
    }

    @Test
    void parsesConfigMessageWithTtsToggle() throws Exception {
        StreamingControlMessage message = StreamingControlMessage.parse(
                objectMapper,
                "{\"type\":\"config\",\"ttsEnabled\":true}"
        );

        assertThat(message.isConfig()).isTrue();
        assertThat(message.ttsEnabled()).isTrue();
    }
}
