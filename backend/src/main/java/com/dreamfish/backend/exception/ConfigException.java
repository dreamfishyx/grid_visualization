package com.dreamfish.backend.exception;

import com.dreamfish.backend.common.ErrorCode;

/**
 * @author Dream fish
 * @version 1.0
 * @description: 配置异常类, 主要用于配置文件或者配置项的异常
 * @date 2025/4/10 17:04
 */
public class ConfigException extends BusinessException {

    public ConfigException(ErrorCode errorCode) {
        super(errorCode);
    }

    public ConfigException(ErrorCode errorCode, String message) {
        super(errorCode, message);
    }
}
