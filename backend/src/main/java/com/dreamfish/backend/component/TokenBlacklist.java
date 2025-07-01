package com.dreamfish.backend.component;

import com.dreamfish.backend.common.RedisKeyPrefix;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * @author Dream fish
 * @version 1.0
 * @description: 使用Redis作为临时表存储JWT黑名单
 * @date 2025/4/9 18:31
 */
@Component
@RequiredArgsConstructor
public class TokenBlacklist {
    private final RedisTemplate<String, String> redisTemplate;

    private final JwtUtil jwtUtil;

    /**
     * 将 Token 添加到黑名单
     *
     * @param token Token
     */
    public void addToBlacklist(String token) {
        Long ttl = jwtUtil.getRemainingTime(token);
        if (ttl > 0) {
            redisTemplate.opsForValue().set(
                    RedisKeyPrefix.Token_Blacklist.generateFullKey(token),
                    "blacklist",
                    ttl,
                    TimeUnit.MILLISECONDS
            );
        }
    }

    /**
     * 判断 Token 是否在黑名单中
     *
     * @param token Token
     * @return 是否在黑名单中
     */
    public boolean isBlacklisted(String token) {
        return redisTemplate.hasKey(RedisKeyPrefix.Token_Blacklist.generateFullKey(token));
    }
}