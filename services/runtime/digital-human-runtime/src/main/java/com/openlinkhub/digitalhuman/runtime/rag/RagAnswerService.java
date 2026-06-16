package com.openlinkhub.digitalhuman.runtime.rag;

public interface RagAnswerService {

    void streamAnswer(String question, RagAnswerSink sink);
}
