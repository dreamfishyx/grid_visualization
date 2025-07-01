package com.dreamfish.backend.entity.status;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * @author Dream fish
 * @version 1.0
 * @description: 维修记录状态
 * @date 2025/4/12 11:59
 */
@Getter
@AllArgsConstructor
public enum MaintenanceRecordStatus implements BaseStatus {
    UNFINISHED(0, "0:未完成"),
    FINISHED(1, "1:完成");

    private final int code;
    private final String description;

    public static MaintenanceRecordStatus fromCode(int code) {
        return BaseStatus.fromCode(MaintenanceRecordStatus.class, code);
    }
}
