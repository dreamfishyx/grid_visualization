package com.dreamfish.backend.dao;

import com.dreamfish.backend.entity.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * @author Dream fish
 * @version 1.0
 * @description: TODO
 * @date 2025/4/3 14:47
 */
@Mapper
public interface UserMapper {
    /**
     * 根据用户名查询用户
     *
     * @param userName 用户名
     */
    User findByUsername(@Param("userName") String userName);

    /**
     * 新增用户
     *
     * @param user 用户对象
     */
    int insertUser(User user);

    /**
     * 根据邮箱查询用户
     *
     * @param email 邮箱
     */
    User findByEmail(@Param("email") String email);

    /**
     * 根据用户id查询用户
     *
     * @param userId 用户id
     */
    User getById(
            @Param("userId") Integer userId
    );

    /**
     * 更新用户密码
     *
     * @param user 用户对象
     */
    int updatePassword(User user);
}
