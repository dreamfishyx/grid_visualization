package com.dreamfish.sea.oldbook.dao;

import com.dreamfish.sea.oldbook.entity.User;

/**
 * @author Dream fish
 * @version 1.0
 * @description: TODO
 * @date 2023/11/13 13:39
 */
public interface UserMapper {
    User getUserById(Integer id);

    User getUserByName(String name, String password);

    Integer existUserByName(String name);


    Integer addUser(User user);
}
