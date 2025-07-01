package com.dreamfish.backend.service;

import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * @author Dream fish
 * @version 1.0
 * @description: sse service
 * @date 2025/4/24 16:15
 */
public interface SSEService {
    SseEmitter subscribe();
    void sendDataUpdateEvent(String name, String data);
    void sendHeartbeat();
}