package com.dreamfish.backend.entity.status;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * @author Dream fish
 * @version 1.0
 * @description: 消息类型
 * @date 2025/4/26 10:13
 */
@Getter
@AllArgsConstructor
public enum MessageType implements BaseStatus {
    INFO(0, "0:通知"),
    WARNING(1, "1:警告"),
    ERROR(2, "2:错误");

    private final int code;
    private final String description;
    public static MessageType fromCode(int code) {
        return BaseStatus.fromCode(MessageType.class, code);
    }
}
