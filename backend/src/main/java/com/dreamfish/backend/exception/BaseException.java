package com.dreamfish.backend.exception;

import com.dreamfish.backend.common.ErrorCode;
import lombok.Getter;

/**
 * @author Dream fish
 * @version 1.0
 * @description: 统一异常类:属于系统级别或者数据级别的异常,一般前端不需要处理也无法处理
 * @date 2025/4/9 17:04
 */
@Getter
public class BaseException extends RuntimeException {

    private final ErrorCode errorCode;

    public BaseException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }

    public BaseException(ErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public BaseException(ErrorCode errorCode, Throwable cause) {
        super(errorCode.getMessage(), cause);
        this.errorCode = errorCode;
    }

    public BaseException(ErrorCode errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }
}
