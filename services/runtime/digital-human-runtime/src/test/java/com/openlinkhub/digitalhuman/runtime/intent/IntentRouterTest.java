package com.openlinkhub.digitalhuman.runtime.intent;

import static org.assertj.core.api.Assertions.assertThat;

import com.openlinkhub.digitalhuman.runtime.config.IntentRoutingProperties;
import org.junit.jupiter.api.Test;

class IntentRouterTest {

    private final IntentRouter router = new IntentRouter(IntentRoutingProperties.defaults());

    @Test
    void routesForwardCommandAliasesToSameAction() {
        assertForward(router.route("请向前"));
        assertForward(router.route("向前吧"));
        assertForward(router.route("往前走"));
        assertForward(router.route("前进"));
    }

    @Test
    void routesStopSpeechAliasesToTtsStopCommand() {
        assertTtsStop(router.route("停"));
        assertTtsStop(router.route("不用说了"));
        assertTtsStop(router.route("Stop"));
    }

    @Test
    void routesOpenWindowCommandWithTargetSlot() {
        IntentMatch match = router.route("打开监控窗口");

        assertThat(match.kind()).isEqualTo(IntentKind.COMMAND);
        assertThat(match.name()).isEqualTo("window.open");
        assertThat(match.slots()).containsEntry("target", "监控");
    }

    @Test
    void routesPlayMusicCommandWithKeywordSlot() {
        IntentMatch match = router.route("播放周杰伦音乐");

        assertThat(match.kind()).isEqualTo(IntentKind.COMMAND);
        assertThat(match.name()).isEqualTo("music.play");
        assertThat(match.slots()).containsEntry("keyword", "周杰伦");
    }

    @Test
    void routesDateTimeQuestionsToBuiltinTool() {
        IntentMatch match = router.route("今天几号");

        assertThat(match.kind()).isEqualTo(IntentKind.DATE_TIME);
        assertThat(match.name()).isEqualTo("date.today");
    }

    @Test
    void routesWeatherQuestionsToBuiltinTool() {
        IntentMatch match = router.route("未来一周天气情况");

        assertThat(match.kind()).isEqualTo(IntentKind.WEATHER);
        assertThat(match.name()).isEqualTo("weather.weekly");
    }

    @Test
    void fallsBackToRagForKnowledgeQuestions() {
        IntentMatch match = router.route("江门小学的建设情况");

        assertThat(match.kind()).isEqualTo(IntentKind.RAG);
        assertThat(match.name()).isEqualTo("rag.query");
        assertThat(match.normalizedText()).isEqualTo("江门小学的建设情况");
    }

    private void assertForward(IntentMatch match) {
        assertThat(match.kind()).isEqualTo(IntentKind.COMMAND);
        assertThat(match.name()).isEqualTo("move.forward");
    }

    private void assertTtsStop(IntentMatch match) {
        assertThat(match.kind()).isEqualTo(IntentKind.COMMAND);
        assertThat(match.name()).isEqualTo("tts.stop");
    }
}
