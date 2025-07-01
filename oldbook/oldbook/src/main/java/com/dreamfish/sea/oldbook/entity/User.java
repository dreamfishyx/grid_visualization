package com.dreamfish.sea.oldbook.entity;

import com.dreamfish.sea.oldbook.util.LoginEnv;
import com.dreamfish.sea.oldbook.util.PasswordMatches;
import com.dreamfish.sea.oldbook.util.RegisterEnv;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;
import org.hibernate.validator.constraints.Length;

/**
 * @author Dream fish
 * @version 1.0
 * @description: TODO
 * @date 2023/11/13 13:40
 */
@Data
@PasswordMatches(groups = {RegisterEnv.class})
public class User {
    Integer userId;
    @NotEmpty(message = "用户名不能为空", groups = {LoginEnv.class, RegisterEnv.class}) @Length(min = 1, max = 30, message = "用户名长度必须在1-30之间", groups = {RegisterEnv.class}) String userName;
    @NotEmpty(message = "密码不能为空", groups = {LoginEnv.class, RegisterEnv.class}) String password;
    @NotEmpty(message = "确认密码不能为空", groups = {RegisterEnv.class}) String matchingPassword;
    Integer gender;
}
