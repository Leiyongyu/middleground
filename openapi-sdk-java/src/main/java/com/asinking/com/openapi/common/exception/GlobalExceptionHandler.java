package com.asinking.com.openapi.common.exception;

import com.asinking.com.openapi.common.response.Result;
import com.asinking.com.openapi.common.response.ResultCode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 全局异常处理器：
 * - 业务异常 → HTTP 200（body 中携带业务错误码）
 * - 参数校验 → HTTP 400
 * - 数据冲突 → HTTP 409
 * - 未知异常 → HTTP 500
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger LOG = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /** 业务异常：HTTP 200，body 中携带具体业务错误码 */
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<Result<?>> handleBusiness(BusinessException e) {
        if (e.getResultCode() == ResultCode.FORBIDDEN) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Result.fail(e.getResultCode(), e.getMessage()));
        }
        return ResponseEntity.ok(Result.fail(e.getResultCode(), e.getMessage()));
    }

    /** 参数校验失败 → HTTP 400 */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Result<?>> handleIllegalArgument(IllegalArgumentException e) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Result.fail(ResultCode.BAD_REQUEST, e.getMessage()));
    }

    /** 数据库唯一键冲突 → HTTP 409 */
    @ExceptionHandler(DuplicateKeyException.class)
    public ResponseEntity<Result<?>> handleDuplicateKey(DuplicateKeyException e) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(Result.fail(ResultCode.CONFLICT, "数据已存在"));
    }

    /** 未捕获的异常 → HTTP 500，记录日志 */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Result<?>> handleException(Exception e) {
        LOG.error("未处理异常", e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Result.fail(ResultCode.SERVER_ERROR, "服务器内部错误"));
    }
}
