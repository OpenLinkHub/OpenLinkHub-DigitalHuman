package com.openlinkhub.digitalhuman.runtime.intent;

import java.util.Collections;
import java.util.Map;

public final class IntentMatch {

    private final IntentKind kind;
    private final String name;
    private final String normalizedText;
    private final Map<String, String> slots;
    private final double confidence;

    private IntentMatch(IntentKind kind, String name, String normalizedText, Map<String, String> slots, double confidence) {
        this.kind = kind;
        this.name = name;
        this.normalizedText = normalizedText;
        this.slots = slots;
        this.confidence = confidence;
    }

    public static IntentMatch command(String name, String normalizedText, Map<String, String> slots) {
        return new IntentMatch(IntentKind.COMMAND, name, normalizedText, slots, 1.0);
    }

    public static IntentMatch dateTime(String name, String normalizedText) {
        return new IntentMatch(IntentKind.DATE_TIME, name, normalizedText, Collections.<String, String>emptyMap(), 1.0);
    }

    public static IntentMatch weather(String name, String normalizedText) {
        return new IntentMatch(IntentKind.WEATHER, name, normalizedText, Collections.<String, String>emptyMap(), 1.0);
    }

    public static IntentMatch rag(String normalizedText) {
        return new IntentMatch(IntentKind.RAG, "rag.query", normalizedText, Collections.<String, String>emptyMap(), 0.5);
    }

    public IntentKind kind() {
        return kind;
    }

    public String name() {
        return name;
    }

    public String normalizedText() {
        return normalizedText;
    }

    public Map<String, String> slots() {
        return slots;
    }

    public double confidence() {
        return confidence;
    }
}
