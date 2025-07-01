package com.dreamfish.backend.exception;

import com.dreamfish.backend.common.ErrorCode;

/**
 * @author Dream fish
 * @version 1.0
 * @description: 用户异常
 * @date 2025/4/12 20:38
 */
public class UserException extends BusinessException {
    public UserException(ErrorCode errorCode) {
        super(errorCode);
    }

    public UserException(ErrorCode errorCode, String message) {
        super(errorCode, message);
    }

    public UserException(ErrorCode errorCode, Throwable cause) {
        super(errorCode, cause);
    }

    public UserException(ErrorCode errorCode, String message, Throwable cause) {
        super(errorCode, message, cause);
    }
}
