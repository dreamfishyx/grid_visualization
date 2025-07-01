package com.dreamfish.backend.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

/**
 * @author Dream fish
 * @version 1.0
 * @description: 设备维修人员释放事件
 * @date 2025/4/11 20:13
 */
@Getter
public class WorkerReleasedEvent extends ApplicationEvent {
    private final Integer userId;

    public WorkerReleasedEvent(Object source, Integer userId) {
        super(source);
        this.userId = userId;
    }

}