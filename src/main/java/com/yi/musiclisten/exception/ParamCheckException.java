package com.yi.musiclisten.exception;

public class ParamCheckException extends RuntimeException {
    private final int code;

    public ParamCheckException(int code, String message) {
        super(message);
        this.code = code;
    }

    public ParamCheckException(String message) {
        this(400, message); // 默认 400
    }

    public int getCode() {
        return code;
    }
}
