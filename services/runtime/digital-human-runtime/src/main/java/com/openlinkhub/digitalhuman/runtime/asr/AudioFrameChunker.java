package com.openlinkhub.digitalhuman.runtime.asr;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

public final class AudioFrameChunker {

    private AudioFrameChunker() {
    }

    public static List<ByteBuffer> chunk(byte[] audio, int chunkSizeBytes) {
        if (audio == null || audio.length == 0) {
            return List.of();
        }
        if (chunkSizeBytes <= 0) {
            throw new IllegalArgumentException("chunkSizeBytes must be greater than 0");
        }

        List<ByteBuffer> frames = new ArrayList<>((audio.length + chunkSizeBytes - 1) / chunkSizeBytes);
        for (int offset = 0; offset < audio.length; offset += chunkSizeBytes) {
            int length = Math.min(chunkSizeBytes, audio.length - offset);
            frames.add(ByteBuffer.wrap(audio, offset, length));
        }
        return frames;
    }
}
