package com.dreamfish.backend.common;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * @author Dream fish
 * @version 1.0
 * @description: redis临时表前缀枚举类
 * @date 2025/4/10 11:10
 */
@AllArgsConstructor
@Getter
public enum RedisKeyPrefix {
    Token_Blacklist("token黑名单", "token:blacklist:"),
    Device_Registration("设备注册临时表", "device:register:"),
    VERIFICATION_CODE("验证码临时表", "verification:code:"),
    DEVICE_HEART_BEAT("设备心跳临时表", "device:heartbeat:"),
    MAINTENANCE_QUEUE("维修队列列表", "maintenance:queue:"),
    DEVICE_CACHE("设备缓存", "device:cache:");

    private final String description;

    private final String prefix;

    /**
     * 根据key进行前缀拼接,生成完整的key
     *
     * @param key 原始key
     * @return 完整的key
     */
    public String generateFullKey(String key) {
        return this.prefix + key;
    }

    /**
     * 根据前缀和拼接的key获取原始key
     *
     * @param key 完整的key
     * @return 原始key
     */
    public String parseOriginalKey(String key) {
        return key.substring(this.prefix.length());
    }
}
