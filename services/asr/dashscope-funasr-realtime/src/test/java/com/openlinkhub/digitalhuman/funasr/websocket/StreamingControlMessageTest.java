package com.openlinkhub.digitalhuman.funasr.websocket;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

class StreamingControlMessageTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void parsesStartMessageWithSampleRate() throws Exception {
        StreamingControlMessage message = StreamingControlMessage.parse(
                objectMapper,
                "{\"type\":\"start\",\"sampleRate\":16000}"
        );

        assertThat(message.isStart()).isTrue();
        assertThat(message.isStop()).isFalse();
        assertThat(message.sampleRate()).isEqualTo(16000);
    }

    @Test
    void parsesStopMessageWithoutSampleRate() throws Exception {
        StreamingControlMessage message = StreamingControlMessage.parse(objectMapper, "{\"type\":\"stop\"}");

        assertThat(message.isStop()).isTrue();
        assertThat(message.sampleRate()).isNull();
    }
}
