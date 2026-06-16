package com.openlinkhub.digitalhuman.runtime.orchestration;

import com.openlinkhub.digitalhuman.runtime.intent.IntentKind;
import com.openlinkhub.digitalhuman.runtime.intent.IntentMatch;
import com.openlinkhub.digitalhuman.runtime.intent.IntentRouter;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class ConversationOrchestrator {

    private final IntentRouter intentRouter;
    private final DateTimeTool dateTimeTool;
    private final WeatherTool weatherTool;

    public ConversationOrchestrator(IntentRouter intentRouter, DateTimeTool dateTimeTool, WeatherTool weatherTool) {
        this.intentRouter = intentRouter;
        this.dateTimeTool = dateTimeTool;
        this.weatherTool = weatherTool;
    }

    public void handle(String text, ConversationResponseSink sink) {
        IntentMatch match = intentRouter.route(text);
        emitIntentResult(match, sink);
        if (match.kind() == IntentKind.RAG) {
            if (effectiveQueryLength(match.normalizedText()) < 3) {
                emitRagRejected(match, sink);
                return;
            }
            sink.queryRag(match.normalizedText());
            return;
        }
        if (match.kind() == IntentKind.COMMAND) {
            if ("tts.stop".equals(match.name())) {
                handleTtsStop(sink);
                return;
            }
            handleCommand(match, sink);
            return;
        }
        if (match.kind() == IntentKind.DATE_TIME) {
            emitToolResult(match.name(), dateTimeTool.today(), sink);
            return;
        }
        if (match.kind() == IntentKind.WEATHER) {
            String textResult = "weather.weekly".equals(match.name()) ? weatherTool.weekly() : weatherTool.today();
            emitToolResult(match.name(), textResult, sink);
        }
    }

    private void emitIntentResult(IntentMatch match, ConversationResponseSink sink) {
        Map<String, Object> event = new LinkedHashMap<String, Object>();
        event.put("type", "intent_result");
        event.put("kind", match.kind().name());
        event.put("name", match.name());
        event.put("normalizedText", match.normalizedText());
        event.put("slots", match.slots());
        event.put("confidence", Double.valueOf(match.confidence()));
        sink.sendEvent(event);
    }

    private void emitRagRejected(IntentMatch match, ConversationResponseSink sink) {
        Map<String, Object> event = new LinkedHashMap<String, Object>();
        event.put("type", "rag_rejected");
        event.put("reason", "query_too_short");
        event.put("text", "知识库查询内容至少 3 个字，请补充完整问题。");
        event.put("query", match.normalizedText());
        sink.sendEvent(event);
    }

    private int effectiveQueryLength(String text) {
        if (text == null) {
            return 0;
        }
        return text.replaceAll("[\\s，。！？、,.!?：:；;“”\"'（）()《》<>\\[\\]]+", "").length();
    }

    private void handleCommand(IntentMatch match, ConversationResponseSink sink) {
        String text = commandAcknowledgement(match);
        Map<String, Object> event = new LinkedHashMap<>();
        event.put("type", "command_result");
        event.put("command", match.name());
        event.put("status", "accepted");
        event.put("text", text);
        event.put("slots", match.slots());
        sink.sendEvent(event);
        sink.speak(text);
    }

    private void handleTtsStop(ConversationResponseSink sink) {
        Map<String, Object> event = new LinkedHashMap<String, Object>();
        event.put("type", "tts_stop");
        event.put("text", "已停止播报。");
        sink.sendEvent(event);
    }

    private void emitToolResult(String intent, String text, ConversationResponseSink sink) {
        Map<String, Object> event = new LinkedHashMap<String, Object>();
        event.put("type", "tool_result");
        event.put("intent", intent);
        event.put("text", text);
        sink.sendEvent(event);
        sink.speak(text);
    }

    private String commandAcknowledgement(IntentMatch match) {
        if ("move.forward".equals(match.name())) {
            return "已执行：向前。";
        }
        if ("turn.left".equals(match.name())) {
            return "已执行：向左。";
        }
        if ("turn.right".equals(match.name())) {
            return "已执行：向右。";
        }
        if ("video.open".equals(match.name())) {
            return "已为你打开视频。";
        }
        if ("window.open".equals(match.name())) {
            return "已为你打开" + slotOrDefault(match, "target", "目标") + "窗口。";
        }
        if ("music.play".equals(match.name())) {
            return "已为你播放" + slotOrDefault(match, "keyword", "指定") + "音乐。";
        }
        if ("music.stop".equals(match.name())) {
            return "已停止播放。";
        }
        return "已接收指令。";
    }

    private String slotOrDefault(IntentMatch match, String name, String fallback) {
        String value = match.slots().get(name);
        return value == null || value.trim().isEmpty() ? fallback : value;
    }
}
