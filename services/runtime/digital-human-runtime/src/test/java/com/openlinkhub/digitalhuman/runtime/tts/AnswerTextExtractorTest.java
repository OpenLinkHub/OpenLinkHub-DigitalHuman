package com.openlinkhub.digitalhuman.runtime.tts;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class AnswerTextExtractorTest {

    @Test
    void extractsSpeechTextWithoutThinkingAndReferences() {
        AnswerTextExtractor.ExtractedAnswer answer = AnswerTextExtractor.extract("""
                <think>内部推理，不应该朗读。</think>
                这是需要展示和播报的正文。

                ### References
                - 文档 A
                - 文档 B
                """);

        assertThat(answer.speechText()).isEqualTo("这是需要展示和播报的正文。");
        assertThat(answer.thinking()).isEqualTo("内部推理，不应该朗读。");
        assertThat(answer.references()).contains("文档 A", "文档 B");
    }

    @Test
    void handlesOpenThinkingBlockDuringStreaming() {
        AnswerTextExtractor.ExtractedAnswer answer = AnswerTextExtractor.extract("<think>仍在思考");

        assertThat(answer.speechText()).isBlank();
        assertThat(answer.thinking()).isEqualTo("仍在思考");
    }
}
