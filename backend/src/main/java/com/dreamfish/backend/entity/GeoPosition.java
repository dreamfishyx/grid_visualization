package com.dreamfish.backend.entity;

import com.dreamfish.backend.parser.annotation.BinaryField;
import com.dreamfish.backend.parser.group.Register;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.math.BigDecimal;

/**
 * @author Dream fish
 * @version 1.0
 * @description: 地理位置类
 * @date 2025/2/19 16:17
 */
@Data
@NoArgsConstructor
@Accessors(chain = true)
@Schema(description = "地理信息核心数据模型", title = "GeoPosition")
public class GeoPosition {

    // 经度
    @BinaryField(order = 1, length = 4, scale = 6, groups = {Register.class})
    private BigDecimal longitude;

    // 纬度
    @BinaryField(order = 2, length = 4, scale = 6, groups = {Register.class})
    private BigDecimal latitude;

}
