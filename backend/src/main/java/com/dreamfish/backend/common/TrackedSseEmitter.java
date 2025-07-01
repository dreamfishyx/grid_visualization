package com.dreamfish.backend.common;

import org.jetbrains.annotations.NotNull;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * @author Dream fish
 * @version 1.0
 * @description: 拓展sse emitter: 添加超时、完成标记
 * @date 2025/4/27 12:52
 */
public class TrackedSseEmitter extends SseEmitter {
    private volatile boolean isCompleted = false;
    private volatile boolean isTimedOut = false;

    public TrackedSseEmitter(Long timeout) {
        super(timeout);
    }

    @Override
    public void complete() {
        if (!isCompleted) {
            super.complete();
            isCompleted = true;
        }
    }

    @Override
    public void completeWithError(@NotNull Throwable ex) {
        if (!isCompleted) {
            super.completeWithError(ex);
            isCompleted = true;
        }
    }

    /**
     * 判断是否完成
     *
     * @return 是否完成
     */
    public boolean isCompleted() {
        return isCompleted && isTimedOut;
    }

    /**
     * 标记超时
     */
    public void markTimeout() {
        isTimedOut = true;
    }
}