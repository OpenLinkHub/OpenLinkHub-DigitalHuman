package com.openlinkhub.digitalhuman.funasr.service;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class AudioFormatResolverTest {

    @Test
    void detectsM4aUploadsThatNeedPcmTranscoding() {
        boolean transcode = AudioFormatResolver.shouldTranscodeToPcm(null, "测试音频.m4a", "audio/x-m4a");

        assertThat(transcode).isTrue();
    }

    @Test
    void keepsExplicitFormatOverride() {
        String format = AudioFormatResolver.resolve("wav", "测试音频.m4a", "audio/x-m4a", "pcm");

        assertThat(format).isEqualTo("wav");
    }
}
