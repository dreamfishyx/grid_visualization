package com.dreamfish.backend.entity;

import com.dreamfish.backend.entity.status.MessageStatus;
import com.dreamfish.backend.entity.status.MessageType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.experimental.Accessors;

import java.util.Date;


/**
 * @author Dream fish
 * @version 1.0
 * @description: 消息通知类
 * @date 2025/4/11 14:54
 */
@Data
@Schema(description = "消息通知核心数据模型",title = "Message")
@Accessors(chain = true)
public class Message {
    // 消息id
    private Integer messageId;
    // 用户id
    private Integer userId;

    // 消息类型
    private MessageType type;

    // 标题
    private String title;
    // 内容
    private String content;
    // 是否已读
    MessageStatus hasRead;
    // 创建时间
    Date createTime;
}
