package com.openlinkhub.digitalhuman.funasr.exception;

public class FunAsrException extends RuntimeException {

    public FunAsrException(String message) {
        super(message);
    }

    public FunAsrException(String message, Throwable cause) {
        super(message, cause);
    }
}
