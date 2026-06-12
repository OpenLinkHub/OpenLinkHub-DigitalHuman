package com.openlinkhub.digitalhuman.funasr.service;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class AudioFrameChunkerTest {

    @Test
    void chunksAudioBytesWithoutDroppingTailBytes() {
        byte[] audio = new byte[] {1, 2, 3, 4, 5, 6, 7};

        var frames = AudioFrameChunker.chunk(audio, 3);

        assertThat(frames).hasSize(3);
        assertThat(toBytes(frames.get(0))).containsExactly(1, 2, 3);
        assertThat(toBytes(frames.get(1))).containsExactly(4, 5, 6);
        assertThat(toBytes(frames.get(2))).containsExactly(7);
    }

    @Test
    void returnsNoFramesForEmptyAudio() {
        assertThat(AudioFrameChunker.chunk(new byte[0], 3200)).isEmpty();
    }

    private byte[] toBytes(java.nio.ByteBuffer buffer) {
        byte[] bytes = new byte[buffer.remaining()];
        buffer.duplicate().get(bytes);
        return bytes;
    }
}
