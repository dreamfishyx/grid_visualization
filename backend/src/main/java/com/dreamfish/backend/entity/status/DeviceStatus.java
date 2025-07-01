package com.dreamfish.backend.entity.status;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * @author Dream fish
 * @version 1.0
 * @description: 设备状态
 * @date 2025/2/19 16:25
 */
@AllArgsConstructor
@Getter
public enum DeviceStatus implements BaseStatus {
    NORMAL(0, "0:在线"),
    OFFLINE(1, "1:离线"),
    ABNORMAL(2, "2:异常");


    private final int code;
    private final String description;

    public static DeviceStatus fromCode(int code) {
        return BaseStatus.fromCode(DeviceStatus.class, code);
    }
}