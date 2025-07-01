package com.dreamfish.backend.entity;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import org.hibernate.validator.constraints.Length;

/**
 * @author Dream fish
 * @version 1.0
 * @description: TODO
 * @date 2025/4/3 20:57
 */
@Data
@NoArgsConstructor
@Schema(description = "用户信息核心数据模型",title = "User")
@Accessors(chain = true)
public class User {
    // 用户ID
    private Integer userId;
    // 用户名
    @NotBlank(message = "用户名不能为空")
    @Length(min = 3, max = 20, message = "用户名长度在3到20之间")
    private String userName;
    // 密码
    @Length(min = 6, max = 20, message = "密码长度在6到20之间")
    @NotBlank(message = "密码不能为空")
    private String password;
    // 邮箱
    @Email(message = "邮箱格式不正确", regexp = "^[a-zA-Z0-9._-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$")
    private String email;
    // 是否删除
    private Integer isDeleted;
    // 验证码
    private String verifyCode;
}
