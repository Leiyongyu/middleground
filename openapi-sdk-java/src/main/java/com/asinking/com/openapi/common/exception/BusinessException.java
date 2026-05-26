package com.asinking.com.openapi.common.exception;

import com.asinking.com.openapi.common.response.ResultCode;

public class BusinessException extends RuntimeException {

    private final ResultCode resultCode;

    public BusinessException(ResultCode resultCode, String message) {
        super(message);
        this.resultCode = resultCode;
    }

    public BusinessException(ResultCode resultCode) {
        this(resultCode, resultCode.getMessage());
    }

    public ResultCode getResultCode() {
        return resultCode;
    }
}

