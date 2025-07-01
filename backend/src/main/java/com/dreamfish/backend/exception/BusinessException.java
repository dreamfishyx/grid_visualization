package com.dreamfish.backend.exception;

import com.dreamfish.backend.common.ErrorCode;

/**
 * @author Dream fish
 * @version 1.0
 * @description: 业务异常类:会发送到前端,属于业务级别的异常,可以被前端处理
 * @date 2025/4/10 16:06
 */
public class BusinessException extends BaseException {

    public BusinessException(ErrorCode errorCode) {
        super(errorCode);
    }

    public BusinessException(ErrorCode errorCode, String message) {
        super(errorCode, message);
    }

    public BusinessException(ErrorCode errorCode, Throwable cause) {
        super(errorCode, cause);
    }

    public BusinessException(ErrorCode errorCode, String message, Throwable cause) {
        super(errorCode, message, cause);
    }
}
