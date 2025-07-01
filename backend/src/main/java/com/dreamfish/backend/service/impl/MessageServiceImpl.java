package com.dreamfish.backend.service.impl;

import com.dreamfish.backend.common.ErrorCode;
import com.dreamfish.backend.dao.MessageMapper;
import com.dreamfish.backend.entity.Message;
import com.dreamfish.backend.exception.MessageException;
import com.dreamfish.backend.service.MessageService;
import com.dreamfish.backend.service.SSEService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * @author Dream fish
 * @version 1.0
 * @description: 消息通知 service 实现类
 * @date 2025/4/11 18:11
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MessageServiceImpl implements MessageService {

    private final MessageMapper messageMapper;
    private final SSEService sseService;

    @Override
    public List<Message> findAllUnreadMessages(Integer userId) {
        return messageMapper.findAllUnreadMessages(userId);
    }

    @Override
    public void saveMessage(Message message) {
        // 保存消息
        messageMapper.insertMessage(message);

        // 推送消息
        sendUpdateMessage(message.getUserId());
    }

    @Override
    public void updateRead(Message message) {
        int i = messageMapper.updateMessageStatus(message);
        if (i == 0) {
            throw new MessageException(
                    ErrorCode.DB_OPERATION_FAILED, "更新失败"
            );
        }

        // 推送消息
        sendUpdateMessage(message.getUserId());
    }

    private void sendUpdateMessage(Integer userId) {
        log.info("检测到通知更新,即将通知前端更新数据");
        // 提醒前端更新数据
        sseService.sendDataUpdateEvent(
                userId + "", "message-update"
        );
    }

}
