package com.openlinkhub.digitalhuman.runtime.intent;

import com.openlinkhub.digitalhuman.runtime.config.IntentRoutingProperties;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class IntentRouter {

    private static final Pattern OPEN_WINDOW_PATTERN = Pattern.compile("^打开(.+?)窗口$");
    private static final Pattern PLAY_MUSIC_PATTERN = Pattern.compile("^播放(.+?)音乐$");

    private final IntentRoutingProperties properties;

    public IntentRouter(IntentRoutingProperties properties) {
        this.properties = properties;
    }

    public IntentMatch route(String text) {
        String normalized = normalize(text);
        if (normalized.trim().isEmpty()) {
            return IntentMatch.rag(normalized);
        }

        IntentMatch commandMatch = routeCommand(normalized);
        if (commandMatch != null) {
            return commandMatch;
        }
        if (matchesAny(normalized, properties.dateTodayAliases())) {
            return IntentMatch.dateTime("date.today", text.trim());
        }
        if (matchesAny(normalized, properties.weatherWeeklyAliases())) {
            return IntentMatch.weather("weather.weekly", text.trim());
        }
        if (matchesAny(normalized, properties.weatherTodayAliases())) {
            return IntentMatch.weather("weather.today", text.trim());
        }
        return IntentMatch.rag(text.trim());
    }

    private IntentMatch routeCommand(String normalized) {
        Matcher windowMatcher = OPEN_WINDOW_PATTERN.matcher(normalized);
        if (windowMatcher.matches()) {
            Map<String, String> slots = new HashMap<String, String>();
            slots.put("target", windowMatcher.group(1));
            return IntentMatch.command("window.open", normalized, slots);
        }

        Matcher musicMatcher = PLAY_MUSIC_PATTERN.matcher(normalized);
        if (musicMatcher.matches()) {
            Map<String, String> slots = new HashMap<String, String>();
            slots.put("keyword", musicMatcher.group(1));
            return IntentMatch.command("music.play", normalized, slots);
        }

        IntentRoutingProperties.CommandRule bestRule = null;
        int bestAliasLength = -1;
        for (IntentRoutingProperties.CommandRule rule : properties.commands()) {
            if (rule.aliases() == null) {
                continue;
            }
            for (String alias : rule.aliases()) {
                String normalizedAlias = normalize(alias);
                if (commandMatches(normalized, normalizedAlias) && normalizedAlias.length() > bestAliasLength) {
                    bestRule = rule;
                    bestAliasLength = normalizedAlias.length();
                }
            }
        }
        if (bestRule == null) {
            return null;
        }
        return IntentMatch.command(bestRule.name(), normalized, Collections.<String, String>emptyMap());
    }

    private boolean commandMatches(String normalized, String alias) {
        return normalized.equals(alias)
                || stripPoliteWords(normalized).equals(alias)
                || normalized.contains(alias);
    }

    private boolean matchesAny(String normalized, Iterable<String> aliases) {
        for (String alias : aliases) {
            String normalizedAlias = normalize(alias);
            if (normalized.equals(normalizedAlias) || normalized.contains(normalizedAlias)) {
                return true;
            }
        }
        return false;
    }

    private String normalize(String text) {
        if (text == null) {
            return "";
        }
        return stripPoliteWords(text)
                .replaceAll("[\\s，。！？、,.!?：:；;“”\"'（）()《》<>\\[\\]]+", "")
                .trim();
    }

    private String stripPoliteWords(String text) {
        return (text == null ? "" : text.trim())
                .replaceAll("^请+", "")
                .replaceAll("(吧|一下|好吗|可以吗|谢谢)$", "");
    }
}
