package com.dreamfish.backend.exception;

import com.dreamfish.backend.common.ErrorCode;

/**
 * @author Dream fish
 * @version 1.0
 * @description: 地图服务异常
 * @date 2025/4/12 14:37
 */
public class MapServiceException extends BusinessException {

    public MapServiceException(ErrorCode errorCode) {
        super(errorCode);
    }

    public MapServiceException(ErrorCode errorCode, String message) {
        super(errorCode, message);
    }

    public MapServiceException(ErrorCode errorCode, Throwable cause) {
        super(errorCode, cause);
    }

    public MapServiceException(ErrorCode errorCode, String message, Throwable cause) {
        super(errorCode, message, cause);
    }
}

