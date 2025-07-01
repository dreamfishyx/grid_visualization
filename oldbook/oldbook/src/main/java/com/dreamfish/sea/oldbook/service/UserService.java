package com.dreamfish.sea.oldbook.service;

import com.dreamfish.sea.oldbook.entity.User;
import org.springframework.stereotype.Service;

/**
 * @author Dream fish
 * @version 1.0
 * @description: TODO
 * @date 2023/11/13 13:35
 */
@Service
public interface UserService {
    User getUserById(Integer id);

    User getUserByName(String name, String password);

    Boolean exist(String name);

    Boolean addUser(User user);
}
