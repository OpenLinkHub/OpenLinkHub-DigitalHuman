package com.openlinkhub.digitalhuman.runtime.rag;

public interface RagAnswerSink {

    void onStart(String question);

    void onDelta(String delta);

    void onCompleted(String answer);

    void onError(String message);
}
