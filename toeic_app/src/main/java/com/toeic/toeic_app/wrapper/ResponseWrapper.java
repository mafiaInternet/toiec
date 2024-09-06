package com.toeic.toeic_app.wrapper;

public class ResponseWrapper<T> {
    private T content;
    private int code;

    public ResponseWrapper(T content, int code) {
        this.content = content;
        this.code = code;
    }

    // Getters and setters
    public T getContent() {
        return content;
    }

    public void setContent(T content) {
        this.content = content;
    }

    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }
}
