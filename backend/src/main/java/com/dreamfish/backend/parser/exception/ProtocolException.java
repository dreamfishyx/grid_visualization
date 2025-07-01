package com.dreamfish.backend.parser.exception;

/**
 * @author Dream fish
 * @version 1.0
 * @description: 协议异常基类
 * @date 2025/4/3 15:43
 */
public class ProtocolException extends RuntimeException {
    public ProtocolException(String message) {
        super(message);
    }

    public ProtocolException(String message, Throwable cause) {
        super(message, cause);
    }
}

