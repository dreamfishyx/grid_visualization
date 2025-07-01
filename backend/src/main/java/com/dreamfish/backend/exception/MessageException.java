package com.dreamfish.backend.exception;

import com.dreamfish.backend.common.ErrorCode;
/**
 * @author Dream fish
 * @version 1.0
 * @description: 维修记录异常
 * @date 2025/4/28 17:33
 */
public class MessageException extends BusinessException {
    public MessageException(ErrorCode errorCode, String message) {
        super(errorCode, message);
    }

    public MessageException(ErrorCode errorCode) {
        super(errorCode);
    }
}
