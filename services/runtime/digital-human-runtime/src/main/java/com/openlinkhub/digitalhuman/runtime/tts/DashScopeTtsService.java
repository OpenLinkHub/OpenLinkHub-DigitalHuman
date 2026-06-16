package com.openlinkhub.digitalhuman.runtime.tts;

import com.alibaba.dashscope.audio.ttsv2.SpeechSynthesisAudioFormat;
import com.alibaba.dashscope.audio.ttsv2.SpeechSynthesisParam;
import com.alibaba.dashscope.audio.ttsv2.SpeechSynthesizer;
import com.openlinkhub.digitalhuman.runtime.common.exception.FunAsrException;
import com.openlinkhub.digitalhuman.runtime.config.FunAsrProperties;
import com.openlinkhub.digitalhuman.runtime.config.TtsProperties;
import org.springframework.stereotype.Service;

import java.nio.ByteBuffer;
import java.util.Locale;

@Service
public class DashScopeTtsService implements TtsService {

    private final TtsProperties properties;
    private final FunAsrProperties funAsrProperties;

    public DashScopeTtsService(TtsProperties properties, FunAsrProperties funAsrProperties) {
        this.properties = properties;
        this.funAsrProperties = funAsrProperties;
    }

    @Override
    public boolean isEnabled() {
        return properties.enabled();
    }

    @Override
    public TtsAudio synthesize(String text) {
        if (!properties.enabled()) {
            throw new FunAsrException("DashScope TTS is disabled.");
        }
        if (text == null || text.isBlank()) {
            throw new FunAsrException("TTS text is empty.");
        }
        String apiKey = resolveApiKey();
        if (apiKey == null || apiKey.isBlank()) {
            throw new FunAsrException("DashScope TTS API key is missing. Set dashscope.tts.api-key or dashscope.funasr.api-key.");
        }

        SpeechSynthesisAudioFormat format = resolveFormat(properties.format());
        SpeechSynthesisParam param = SpeechSynthesisParam.builder()
                .apiKey(apiKey)
                .model(properties.model())
                .voice(properties.voice())
                .format(format)
                .volume(properties.volume())
                .speechRate(properties.speechRate())
                .pitchRate(properties.pitchRate())
                .build();
        SpeechSynthesizer synthesizer = new SpeechSynthesizer(param, null);
        ByteBuffer audio = synthesizer.call(text, properties.timeoutSeconds() * 1000);
        return new TtsAudio(toBytes(audio), mimeType(format), format.name());
    }

    private String resolveApiKey() {
        if (properties.hasApiKey()) {
            return properties.apiKey();
        }
        if (funAsrProperties.hasApiKey()) {
            return funAsrProperties.apiKey();
        }
        return null;
    }

    private SpeechSynthesisAudioFormat resolveFormat(String configuredFormat) {
        try {
            return SpeechSynthesisAudioFormat.valueOf(configuredFormat.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            throw new FunAsrException("Unsupported DashScope TTS audio format: " + configuredFormat, exception);
        }
    }

    private byte[] toBytes(ByteBuffer buffer) {
        ByteBuffer duplicate = buffer.asReadOnlyBuffer();
        byte[] bytes = new byte[duplicate.remaining()];
        duplicate.get(bytes);
        return bytes;
    }

    private String mimeType(SpeechSynthesisAudioFormat format) {
        String dashScopeFormat = format.getFormat();
        if ("mp3".equalsIgnoreCase(dashScopeFormat)) {
            return "audio/mpeg";
        }
        if ("wav".equalsIgnoreCase(dashScopeFormat)) {
            return "audio/wav";
        }
        if ("ogg".equalsIgnoreCase(dashScopeFormat) || "opus".equalsIgnoreCase(dashScopeFormat)) {
            return "audio/ogg";
        }
        return "application/octet-stream";
    }
}
