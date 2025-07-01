package com.dreamfish.backend.entity;

import com.dreamfish.backend.entity.status.Gender;
import com.dreamfish.backend.entity.status.WorkerStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import org.hibernate.validator.constraints.Length;

/**
 * @author Dream fish
 * @version 1.0
 * @description: 故障维修人员
 * @date 2025/2/19 16:25
 */
@Data
@NoArgsConstructor
@Schema(description = "故障维修人员核心数据模型", title = "Worker")
@Accessors(chain = true)
public class Worker {
    private Integer workerId;
    @Length(min = 2, max = 20, message = "姓名长度在2到20之间")
    private String name;
    private Gender gender;
    @Email(message = "邮箱格式不正确")
    private String email;
    private Long version;
    private Integer userId;
    private WorkerStatus status;
}
