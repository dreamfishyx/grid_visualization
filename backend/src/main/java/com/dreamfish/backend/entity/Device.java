package com.dreamfish.backend.entity;

import com.dreamfish.backend.entity.status.DeviceStatus;
import com.dreamfish.backend.parser.annotation.BinaryField;
import com.dreamfish.backend.parser.group.Maintenance;
import com.dreamfish.backend.parser.group.Receive;
import com.dreamfish.backend.parser.group.Register;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import org.hibernate.validator.constraints.Length;

import java.sql.Date;

/**
 * @author Dream fish
 * @version 1.0
 * @description: TODO
 * @date 2025/2/20 16:54
 */
@NoArgsConstructor
@Data
@Accessors(chain = true)
@Schema(description = "设备信息核心数据模型",title = "Device")
public class Device {
    // ID
//    private Integer id;

    // 所属用户ID
    private Integer userId;

    //是否删除
    private Integer isDeleted;

    // 设备ID
    @NotNull(message = "设备ID不能为空")
    @BinaryField(order = 1, length = 4, groups = {Receive.class, Register.class, Maintenance.class})
    private Integer deviceId;

    // 地理位置
    @BinaryField(order = 4, nested = true,
            groups = {Register.class})
    private GeoPosition geoPosition;

    // 设备状态
    private DeviceStatus status;

    //电阻
    @BinaryField(order = 3, length = 2, scale = 2,
            groups = {Receive.class})
    private Float resistance;


    // 接受数据的时间:timestampNanos
    private Long receiveTime;

    // apiKey
    @NotBlank(message = "API密钥不能为空")
    @Length(min = 16, max = 16, message = "API密钥长度必须为16位")
    @BinaryField(order = 2, length = 16, groups = {Receive.class, Register.class, Maintenance.class})
    private String apiKey;

    // 维修状态
//    private boolean hasMaintenance;

    private Long version;

    // 所处城市
    private String city;

    // 完整地址
    private String fullAddress;

    // 设备描述
    private String description;

    // 注册时间
    private Date registerTime;
}
