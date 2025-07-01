package com.dreamfish.backend.controller;

import com.dreamfish.backend.common.ErrorCode;
import com.dreamfish.backend.common.Result;
import com.dreamfish.backend.entity.CustomUser;
import com.dreamfish.backend.entity.Message;
import com.dreamfish.backend.entity.status.MessageStatus;
import com.dreamfish.backend.service.MessageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author Dream fish
 * @version 1.0
 * @description: 消息通知
 * @date 2025/4/11 15:15
 */
@RestController
@RequestMapping("/app/message")
@Slf4j
@Tag(
        name = "消息通知",
        description = "获取未读消息通知、设置消息为已读"
)
@SecurityRequirement(name = "BearerAuth")
@RequiredArgsConstructor
public class MessageController {
    private final MessageService messageService;

    @Operation(
            summary = "获取未读消息通知:按照时间排序",
            description = "获取未读消息通知,需要 Token。"
    )
    @GetMapping("/list")
    public Result<?> listUnread(
            @AuthenticationPrincipal CustomUser customUser
    ) {
        return Result.success(messageService.findAllUnreadMessages(customUser.getUserId()));
    }

    @Operation(
            summary = "设置消息为已读",
            description = "设置消息为已读,需要 Token。",
            parameters = {
                    @Parameter(
                            name = "messageId",
                            description = "消息ID",
                            required = true
                    )
            }
    )
    @GetMapping("/read")
    public Result<?> read(
            Integer messageId,
            @AuthenticationPrincipal CustomUser customUser
    ) {
        if (messageId == null) {
            return Result.error(HttpStatus.BAD_REQUEST, ErrorCode.PARAMETER_ILLEGAL, "消息ID不能为空");
        }

        messageService.updateRead(
                new Message().setMessageId(messageId).setHasRead(MessageStatus.READ).setUserId(
                        customUser.getUserId()
                )
        );

        return Result.success();
    }


}
