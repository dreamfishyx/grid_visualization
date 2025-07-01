package com.dreamfish.backend.exception;

import com.dreamfish.backend.common.ErrorCode;

/**
 * @author Dream fish
 * @version 1.0
 * @description: 设备异常类:用于处理设备相关的异常
 * @date 2025/4/10 12:17
 */
public class DeviceException extends BusinessException {
    public DeviceException(ErrorCode errorCode, String message) {
        super(errorCode, message);
    }

    public DeviceException(ErrorCode errorCode) {
        super(errorCode);
    }
}
