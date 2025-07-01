package com.dreamfish.backend.dao;

import com.dreamfish.backend.entity.Message;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * @author Dream fish
 * @version 1.0
 * @description: TODO
 * @date 2025/4/11 18:02
 */
@Mapper
public interface MessageMapper {
    List<Message> findAllUnreadMessages(@Param("userId") Integer userId);

    int insertMessage(Message message);

    int updateMessageStatus(Message message);
}
