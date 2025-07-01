package com.dreamfish.backend.service;

import com.dreamfish.backend.entity.Message;

import java.util.List;

/**
 * @author Dream fish
 * @version 1.0
 * @description: 通知服务
 * @date 2025/4/11 18:10
 */
public interface MessageService {

    /**
     * 获取未读通知列表
     *
     * @param userId 用户id
     * @return 通知列表
     */
    List<Message> findAllUnreadMessages(Integer userId);

    /**
     * 新增通知
     *
     * @param message 通知信息
     */
    void saveMessage(Message message);

    /**
     * 设置通知已读
     *
     * @param message 通知信息
     */
    void updateRead(Message message);
}
