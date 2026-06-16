package com.openlinkhub.digitalhuman.runtime.tts;

public record TtsAudio(byte[] bytes, String mimeType, String format) {
}
