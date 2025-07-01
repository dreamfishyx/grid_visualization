package com.dreamfish.backend.exception;

import com.dreamfish.backend.common.ErrorCode;

/**
 * @author Dream fish
 * @version 1.0
 * @description: 数据库操作异常
 * @date 2025/4/15 15:04
 */
public class DbOperationFailedException extends BaseException {
    public DbOperationFailedException(ErrorCode errorCode) {
        super(errorCode);
    }

    public DbOperationFailedException(ErrorCode errorCode, String message) {
        super(errorCode, message);
    }

    public DbOperationFailedException(ErrorCode errorCode, Throwable cause) {
        super(errorCode, cause);
    }

    public DbOperationFailedException(ErrorCode errorCode, String message, Throwable cause) {
        super(errorCode, message, cause);
    }
}
