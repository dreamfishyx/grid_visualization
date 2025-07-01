package com.dreamfish.backend.parser.exception;

/**
 * @author Dream fish
 * @version 1.0
 * @description: 协议解析异常
 * @date 2025/4/3 15:44
 */
public class ProtocolParseException extends ProtocolException {
    public ProtocolParseException(String message) {
        super(message);
    }

    public ProtocolParseException(String message, Throwable cause) {
        super(message, cause);
    }
}