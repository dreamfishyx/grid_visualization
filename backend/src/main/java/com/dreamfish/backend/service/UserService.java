package com.dreamfish.backend.service;

import com.dreamfish.backend.entity.User;

/**
 * @author Dream fish
 * @version 1.0
 * @description: TODO
 * @date 2025/4/9 14:45
 */
public interface UserService {
    /**
     * 注册用户
     *
     * @param user 用户信息
     */
    void register(User user);

    /**
     * 根据名称判断用户是否存在
     *
     * @param username 用户名
     * @return 是否存在
     */
    boolean existsByName(String username);

    /**
     * 判断=邮箱是否存在
     *
     * @param email 邮箱
     * @return 是否存在
     */
    boolean existsByEmail(String email);

    /**
     * 通过id查询用户
     *
     * @param userId 用户id
     * @return 用户信息
     */

    User findById(Integer userId);

    /**
     * 重置密码
     *
     * @param user 用户信息
     */
    void resetPassword(User user);
}
