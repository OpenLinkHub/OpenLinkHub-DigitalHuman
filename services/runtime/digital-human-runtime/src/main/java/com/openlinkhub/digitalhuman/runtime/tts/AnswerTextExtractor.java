package com.openlinkhub.digitalhuman.runtime.tts;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class AnswerTextExtractor {

    private static final Pattern COMPLETE_THINKING_BLOCK = Pattern.compile(
            "<think\\b[^>]*>([\\s\\S]*?)</think>",
            Pattern.CASE_INSENSITIVE
    );
    private static final Pattern OPEN_THINKING_BLOCK = Pattern.compile(
            "<think\\b[^>]*>[\\s\\S]*$",
            Pattern.CASE_INSENSITIVE
    );
    private static final Pattern REFERENCES_HEADING = Pattern.compile(
            "(^|\\R)\\s*(?:#{1,6}\\s*)?References\\s*:?\\s*(\\R|$)",
            Pattern.CASE_INSENSITIVE
    );

    private AnswerTextExtractor() {
    }

    public static ExtractedAnswer extract(String rawAnswer) {
        String working = rawAnswer == null ? "" : rawAnswer;
        List<String> thinkingBlocks = new ArrayList<>();

        Matcher thinkingMatcher = COMPLETE_THINKING_BLOCK.matcher(working);
        StringBuffer withoutThinking = new StringBuffer();
        while (thinkingMatcher.find()) {
            thinkingBlocks.add(cleanup(thinkingMatcher.group(1)));
            thinkingMatcher.appendReplacement(withoutThinking, "");
        }
        thinkingMatcher.appendTail(withoutThinking);
        working = withoutThinking.toString();

        Matcher openThinkingMatcher = OPEN_THINKING_BLOCK.matcher(working);
        if (openThinkingMatcher.find()) {
            thinkingBlocks.add(cleanup(openThinkingMatcher.group().replaceFirst("(?i)<think\\b[^>]*>", "")));
            working = working.substring(0, openThinkingMatcher.start());
        }

        String references = "";
        Matcher referencesMatcher = REFERENCES_HEADING.matcher(working);
        if (referencesMatcher.find()) {
            references = cleanup(working.substring(referencesMatcher.end()));
            working = working.substring(0, referencesMatcher.start());
        }

        String thinking = cleanup(String.join("\n\n", thinkingBlocks));
        return new ExtractedAnswer(cleanup(working), thinking, references);
    }

    private static String cleanup(String text) {
        return (text == null ? "" : text)
                .replaceAll("(?i)</?think\\b[^>]*>", "")
                .replaceAll("\\R{3,}", "\n\n")
                .trim();
    }

    public record ExtractedAnswer(String speechText, String thinking, String references) {
    }
}
