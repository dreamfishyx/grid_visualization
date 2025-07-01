package com.dreamfish.sea.oldbook.service.impl;

import com.dreamfish.sea.oldbook.dao.UserMapper;
import com.dreamfish.sea.oldbook.entity.User;
import com.dreamfish.sea.oldbook.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * @author Dream fish
 * @version 1.0
 * @description: TODO
 * @date 2023/11/13 13:36
 */
@Service
public class UserServiceImpl implements UserService {
    @Autowired
    UserMapper userMapper;

    @Override
    public User getUserById(Integer id) {
        return userMapper.getUserById(id);
    }

    @Override
    public User getUserByName(String name, String password) {
        return userMapper.getUserByName(name, password);
    }

    @Override
    public Boolean exist(String name) {
        return 1 == userMapper.existUserByName(name);
    }

    @Override
    public Boolean addUser(User user) {
        return 1 == userMapper.addUser(user);
    }
}
