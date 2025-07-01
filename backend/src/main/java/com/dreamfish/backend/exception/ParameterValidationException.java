package com.dreamfish.backend.exception;

import com.dreamfish.backend.common.ErrorCode;

/**
 * @author Dream fish
 * @version 1.0
 * @description: 参数校验异常类
 * @date 2025/4/10 12:17
 */
public class ParameterValidationException extends BusinessException {
    public ParameterValidationException(ErrorCode errorCode) {
        super(errorCode);
    }

    public ParameterValidationException(ErrorCode errorCode, String message) {
        super(errorCode, message);
    }

    public ParameterValidationException(ErrorCode errorCode, Throwable cause) {
        super(errorCode, cause);
    }

    public ParameterValidationException(ErrorCode errorCode, String message, Throwable cause) {
        super(errorCode, message, cause);
    }
}
