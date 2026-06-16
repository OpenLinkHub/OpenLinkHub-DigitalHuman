package com.openlinkhub.digitalhuman.runtime.orchestration;

import static org.assertj.core.api.Assertions.assertThat;

import com.openlinkhub.digitalhuman.runtime.config.IntentRoutingProperties;
import com.openlinkhub.digitalhuman.runtime.intent.IntentRouter;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

class ConversationOrchestratorTest {

    private final RecordingSink sink = new RecordingSink();
    private final ConversationOrchestrator orchestrator = new ConversationOrchestrator(
            new IntentRouter(IntentRoutingProperties.defaults()),
            new DateTimeTool(Clock.fixed(Instant.parse("2026-06-16T03:30:00Z"), ZoneId.of("Asia/Shanghai"))),
            new WeatherTool("江门")
    );

    @Test
    void commandIntentEmitsCommandResultAndSpeaksAcknowledgement() {
        orchestrator.handle("往前走", sink);

        assertThat(sink.ragQuestions).isEmpty();
        assertThat(sink.events).hasSize(2);
        assertThat(sink.events.get(0))
                .satisfies(event -> {
                    assertThat(event.get("type")).isEqualTo("intent_result");
                    assertThat(event.get("kind")).isEqualTo("COMMAND");
                    assertThat(event.get("name")).isEqualTo("move.forward");
                });
        assertThat(sink.events.get(1))
                .satisfies(event -> {
                    assertThat(event.get("type")).isEqualTo("command_result");
                    assertThat(event.get("command")).isEqualTo("move.forward");
                    assertThat(event.get("status")).isEqualTo("accepted");
                });
        assertThat(sink.speeches).containsExactly("已执行：向前。");
    }

    @Test
    void stopTtsCommandEmitsStopEventWithoutSpeech() {
        orchestrator.handle("不用说了", sink);

        assertThat(sink.ragQuestions).isEmpty();
        assertThat(sink.events).hasSize(2);
        assertThat(sink.events.get(0))
                .satisfies(event -> {
                    assertThat(event.get("type")).isEqualTo("intent_result");
                    assertThat(event.get("kind")).isEqualTo("COMMAND");
                    assertThat(event.get("name")).isEqualTo("tts.stop");
                });
        assertThat(sink.events.get(1))
                .satisfies(event -> {
                    assertThat(event.get("type")).isEqualTo("tts_stop");
                    assertThat(event.get("text")).isEqualTo("已停止播报。");
                });
        assertThat(sink.speeches).isEmpty();
    }

    @Test
    void dateTimeIntentEmitsToolResultAndSpeaksAnswer() {
        orchestrator.handle("今天几号", sink);

        assertThat(sink.ragQuestions).isEmpty();
        assertThat(sink.events).hasSize(2);
        assertThat(sink.events.get(0))
                .satisfies(event -> {
                    assertThat(event.get("type")).isEqualTo("intent_result");
                    assertThat(event.get("kind")).isEqualTo("DATE_TIME");
                    assertThat(event.get("name")).isEqualTo("date.today");
                });
        assertThat(sink.events.get(1))
                .satisfies(event -> {
                    assertThat(event.get("type")).isEqualTo("tool_result");
                    assertThat(event.get("intent")).isEqualTo("date.today");
                    assertThat(event.get("text").toString()).contains("2026年06月16日");
                });
        assertThat(sink.speeches).singleElement().asString().contains("2026年06月16日");
    }

    @Test
    void weatherIntentEmitsToolUnavailableResultAndSpeaksAnswer() {
        orchestrator.handle("未来一周天气情况", sink);

        assertThat(sink.ragQuestions).isEmpty();
        assertThat(sink.events).hasSize(2);
        assertThat(sink.events.get(0))
                .satisfies(event -> {
                    assertThat(event.get("type")).isEqualTo("intent_result");
                    assertThat(event.get("kind")).isEqualTo("WEATHER");
                    assertThat(event.get("name")).isEqualTo("weather.weekly");
                });
        assertThat(sink.events.get(1))
                .satisfies(event -> {
                    assertThat(event.get("type")).isEqualTo("tool_result");
                    assertThat(event.get("intent")).isEqualTo("weather.weekly");
                    assertThat(event.get("text").toString()).contains("天气服务尚未配置");
                });
        assertThat(sink.speeches).singleElement().asString().contains("天气服务尚未配置");
    }

    @Test
    void ragIntentDelegatesToRagPipeline() {
        orchestrator.handle("江门小学的建设情况", sink);

        assertThat(sink.events).singleElement()
                .satisfies(event -> {
                    assertThat(event.get("type")).isEqualTo("intent_result");
                    assertThat(event.get("kind")).isEqualTo("RAG");
                    assertThat(event.get("name")).isEqualTo("rag.query");
                });
        assertThat(sink.speeches).isEmpty();
        assertThat(sink.ragQuestions).containsExactly("江门小学的建设情况");
    }

    @Test
    void ragIntentShorterThanThreeCharactersReturnsFeedbackWithoutQueryingRag() {
        orchestrator.handle("什。", sink);

        assertThat(sink.ragQuestions).isEmpty();
        assertThat(sink.events).hasSize(2);
        assertThat(sink.events.get(0))
                .satisfies(event -> {
                    assertThat(event.get("type")).isEqualTo("intent_result");
                    assertThat(event.get("kind")).isEqualTo("RAG");
                });
        assertThat(sink.events.get(1))
                .satisfies(event -> {
                    assertThat(event.get("type")).isEqualTo("rag_rejected");
                    assertThat(event.get("reason")).isEqualTo("query_too_short");
                    assertThat(event.get("text").toString()).contains("至少 3 个字");
                });
        assertThat(sink.speeches).isEmpty();
    }

    private static final class RecordingSink implements ConversationResponseSink {
        private final List<Map<String, ?>> events = new ArrayList<>();
        private final List<String> ragQuestions = new ArrayList<>();
        private final List<String> speeches = new ArrayList<>();

        @Override
        public void sendEvent(Map<String, ?> event) {
            events.add(event);
        }

        @Override
        public void queryRag(String question) {
            ragQuestions.add(question);
        }

        @Override
        public void speak(String text) {
            speeches.add(text);
        }
    }
}
