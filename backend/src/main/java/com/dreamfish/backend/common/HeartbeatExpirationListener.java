package com.dreamfish.backend.common;

import com.dreamfish.backend.component.DeviceHeartbeatDispose;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.listener.KeyExpirationEventMessageListener;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;

/**
 * @author Dream fish
 * @version 1.0
 * @description: 设备心跳超时监听
 * @date 2025/4/11 18:53
 */
@Slf4j
public class HeartbeatExpirationListener extends KeyExpirationEventMessageListener {
    private final DeviceHeartbeatDispose deviceHeartbeatDispose;

    public HeartbeatExpirationListener(RedisMessageListenerContainer listenerContainer, DeviceHeartbeatDispose deviceHeartbeatDispose) {
        super(listenerContainer);
        this.deviceHeartbeatDispose = deviceHeartbeatDispose;
    }

    /**
     * 对监听到的消息进行处理。需要 redis 配置: notify-keyspace-events Ex
     *
     * @param message 消息
     * @param pattern 模式
     */
    @Override
    public void onMessage(Message message, byte[] pattern) {
        String key = message.toString();
        if (key.startsWith(RedisKeyPrefix.DEVICE_HEART_BEAT.getPrefix())) {
            log.info("心跳超时:{}", key);
            // 处理会话超时的逻辑
            deviceHeartbeatDispose.dispose(RedisKeyPrefix.DEVICE_HEART_BEAT.parseOriginalKey(key));
        }
    }
}
