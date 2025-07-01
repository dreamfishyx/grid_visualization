package com.dreamfish.backend.controller;

import com.dreamfish.backend.common.ErrorCode;
import com.dreamfish.backend.component.JwtUtil;
import com.dreamfish.backend.component.TokenBlacklist;
import com.dreamfish.backend.exception.BusinessException;
import com.dreamfish.backend.service.SSEService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * @author Dream fish
 * @version 1.0
 * @description: TODO
 * @date 2025/4/24 16:17
 */
@RestController
@Tag(name = "SSE", description = "SSE(用于前端订阅sse)")
@SecurityRequirement(name = "BearerAuth")
@RequiredArgsConstructor
public class SSEController {
    private final SSEService sseService;
    private final JwtUtil jwtUtil;
    private final TokenBlacklist tokenBlacklist;

    @Operation(summary = "订阅 SSE", description = "订阅 SSE")
    @GetMapping(path = "/sse/subscribe", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter subscribe(
            HttpServletRequest request
    ) {

        String token = jwtUtil.parseJwt(request);

        // 验证逻辑
        if (token == null || tokenBlacklist.isBlacklisted(token)) {
            throw new BusinessException(ErrorCode.AUTH_UNAUTHORIZED);
        }
        String username = jwtUtil.extractUsername(token);
        if (!jwtUtil.validateToken(token) || username == null) {
            throw new BusinessException(ErrorCode.AUTH_TOKEN_INVALID);
        }

        // 2. 创建 SSE 连接
        return sseService.subscribe();
    }

}