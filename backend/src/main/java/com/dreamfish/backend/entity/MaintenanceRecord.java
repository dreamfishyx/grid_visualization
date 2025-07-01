package com.dreamfish.backend.entity;

import com.dreamfish.backend.entity.status.MaintenanceRecordStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.experimental.Accessors;
import org.hibernate.validator.constraints.Length;

import java.util.Date;


/**
 * @author Dream fish
 * @version 1.0
 * @description: 检修记录
 * @date 2025/2/19 16:27
 */
@Data
@Schema(description = "维修记录核心数据模型",title = "MaintenanceRecord")
@Accessors(chain = true)
public class MaintenanceRecord {
    // ID
    private Integer id;

    // 设备
//    private Device device;

    // 设备ID
    private Integer deviceId;

    // 所属用户
    private Integer userId;

    // 检修人员ID
    private Worker worker;

    // 状态
    private MaintenanceRecordStatus status;

    // 故障描述
    @Length(min = 1, max = 255, message = "描述长度在1到255之间")
    private String description;

    // 维修过程
    @Length(min = 1, max = 255, message = "维修过程字数在1到255之间")
    private String process;

    // 创建时间
    private Date createdAt;

    // 完成时间
    private Date finishedAt;
}
