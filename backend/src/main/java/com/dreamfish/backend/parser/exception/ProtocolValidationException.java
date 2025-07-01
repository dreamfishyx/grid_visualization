package com.dreamfish.backend.parser.exception;

/**
 * @author Dream fish
 * @version 1.0
 * @description: 注解协议校验异常
 * @date 2025/4/3 15:42
 */
public class ProtocolValidationException extends ProtocolException {
    public ProtocolValidationException(String message) {
        super(message);
    }
}