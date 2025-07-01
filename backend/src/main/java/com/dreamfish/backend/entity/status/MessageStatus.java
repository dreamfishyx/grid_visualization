package com.dreamfish.backend.entity.status;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * @author Dream fish
 * @version 1.0
 * @description: 消息状态
 * @date 2025/4/26 20:09
 */
@Getter
@AllArgsConstructor
public enum MessageStatus implements BaseStatus {
    UNREAD(0, "0:未读"),
    READ(1, "1:已读");

    private final int code;
    private final String description;

    public static MessageStatus fromCode(int code) {
        return BaseStatus.fromCode(MessageStatus.class, code);
    }
}
