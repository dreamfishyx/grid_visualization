package com.dreamfish.backend.service.impl;

import com.dreamfish.backend.common.ErrorCode;
import com.dreamfish.backend.dao.UserMapper;
import com.dreamfish.backend.entity.CustomUser;
import com.dreamfish.backend.entity.User;
import com.dreamfish.backend.exception.UserException;
import com.dreamfish.backend.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

/**
 * @author Dream fish
 * @version 1.0
 * @description: 实现 UserDetailsService接口, 用于加载用户的认证信息
 * @date 2025/4/9 18:07
 */
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserDetailsService, UserService {
    private final UserMapper userMapper;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userMapper.findByUsername(username);
        if (user == null) {
            throw new UsernameNotFoundException("User not found with username: " + username);
        }

        // Collection<GrantedAuthority> authorities = AuthorityUtils.createAuthorityList(user.getRole());

        return new CustomUser(
                user.getUserId(),
                user.getUserName(),
                user.getPassword(),
                user.getEmail()
        );
    }

    @Override
    public void register(User user) {
        userMapper.insertUser(user);
    }

    @Override
    public boolean existsByName(String username) {
        return userMapper.findByUsername(username) != null;
    }

    @Override
    public boolean existsByEmail(String email) {
        return userMapper.findByEmail(email) != null;
    }

    @Override
    public User findById(Integer userId) {
        User user = userMapper.getById(userId);
        if (user == null) {
            throw new UserException(ErrorCode.USER_NOT_FOUND);
        }
        return user;
    }

    @Override
    public void resetPassword(User user) {
        int i = userMapper.updatePassword(user);
        if (i == 0) {
            throw new UserException(ErrorCode.DB_OPERATION_FAILED, "密码重置失败");
        }
    }
}
