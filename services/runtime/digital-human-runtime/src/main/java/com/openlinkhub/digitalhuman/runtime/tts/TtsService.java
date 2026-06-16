package com.openlinkhub.digitalhuman.runtime.tts;

public interface TtsService {

    boolean isEnabled();

    TtsAudio synthesize(String text);
}
