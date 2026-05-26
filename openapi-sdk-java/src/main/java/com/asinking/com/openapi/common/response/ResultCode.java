package com.asinking.com.openapi.common.response;

public enum ResultCode {
    SUCCESS(0, "ok"),
    BAD_REQUEST(400, "bad request"),
    UNAUTHORIZED(401, "unauthorized"),
    CONFLICT(409, "conflict"),
    SERVER_ERROR(500, "server error");

    private final int code;
    private final String message;

    ResultCode(int code, String message) {
        this.code = code;
        this.message = message;
    }

    public int getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }
}
