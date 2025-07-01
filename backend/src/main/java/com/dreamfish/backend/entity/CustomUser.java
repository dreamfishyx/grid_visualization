package com.dreamfish.backend.entity;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import org.springframework.security.core.userdetails.User;

import java.util.Collections;

/**
 * @author Dream fish
 * @version 1.0
 * @description: 继承 security 的 User,增加自定义属性
 * @date 2025/4/11 16:31
 */
@Getter
@Schema(description = "security 用户数据模型(extends User)",title = "CustomUser")
public class CustomUser extends User {

    private final Integer userId;
    private final String email;

    public CustomUser(
            Integer userId,
            String username,
            String password,
            String email
    ) {
        // 传入空权限集合,不需要角色
        super(username, password, Collections.emptyList());
        this.userId = userId;
        this.email = email;
    }

    @Override
    public String getUsername() {
        return super.getUsername();
    }

    @Override
    public String getPassword() {
        return super.getPassword();
    }

}