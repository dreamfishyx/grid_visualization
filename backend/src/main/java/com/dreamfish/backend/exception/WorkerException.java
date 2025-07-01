package com.dreamfish.backend.exception;

import com.dreamfish.backend.common.ErrorCode;

/**
 * @author Dream fish
 * @version 1.0
 * @description: 维修人员异常
 * @date 2025/4/12 20:38
 */
public class WorkerException extends BusinessException {
    public WorkerException(ErrorCode errorCode, String message) {
        super(errorCode, message);
    }

    public WorkerException(ErrorCode errorCode) {
        super(errorCode);
    }
}
