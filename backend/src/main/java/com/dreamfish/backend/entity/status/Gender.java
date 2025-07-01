package com.dreamfish.backend.entity.status;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * @author Dream fish
 * @version 1.0
 * @description: 性别
 * @date 2025/4/26 10:13
 */
@AllArgsConstructor
@Getter
public enum Gender implements BaseStatus {
    MALE(0, "0:男"),
    FEMALE(1, "1:女");

    private final int code;
    private final String description;

    public static Gender fromCode(int code) {
        return BaseStatus.fromCode(Gender.class, code);
    }

}
