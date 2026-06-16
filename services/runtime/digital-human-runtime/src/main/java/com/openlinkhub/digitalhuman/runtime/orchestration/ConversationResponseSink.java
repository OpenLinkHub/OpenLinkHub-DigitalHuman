package com.openlinkhub.digitalhuman.runtime.orchestration;

import java.util.Map;

public interface ConversationResponseSink {

    void sendEvent(Map<String, ?> event);

    void queryRag(String question);

    void speak(String text);
}
