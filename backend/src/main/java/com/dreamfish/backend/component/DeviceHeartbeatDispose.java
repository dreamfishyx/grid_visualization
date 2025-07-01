package com.dreamfish.backend.component;

import com.dreamfish.backend.common.RedisKeyPrefix;
import com.dreamfish.backend.entity.Device;
import com.dreamfish.backend.entity.Message;
import com.dreamfish.backend.entity.User;
import com.dreamfish.backend.entity.status.DeviceStatus;
import com.dreamfish.backend.entity.status.MessageType;
import com.dreamfish.backend.service.DeviceService;
import com.dreamfish.backend.service.EmailService;
import com.dreamfish.backend.service.MessageService;
import com.dreamfish.backend.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * @author Dream fish
 * @version 1.0
 * @description: 设备心跳处理类: 定时更新设备心跳、处理心跳过期设备
 * @date 2025/4/11 18:33
 */
@Component
@RequiredArgsConstructor
public class DeviceHeartbeatDispose {
    private final RedisTemplate<String, Object> redisTemplate;
    private final DeviceService deviceService;
    private final EmailService emailService;
    private final UserService userService;
    private final MessageService messageService;

    @Value("${device.heartbeat.interval}")  // 单位:min
    private long heartbeatInterval;

    /**
     * 更新设备心跳
     *
     * @param deviceId 设备ID
     */
    public void updateRedisHeartbeat(Integer deviceId) {
        if (deviceId == null) {
            // 设备ID为空,不做任何操作
            return;
        }
        if (!deviceService.existsByDeviceId(deviceId)) {
            // 设备不存在,不做任何操作
            return;
        }
        String key = RedisKeyPrefix.DEVICE_HEART_BEAT.generateFullKey(deviceId + "");
        redisTemplate.opsForValue().set(key, "alive", Duration.ofMinutes(heartbeatInterval));
    }

    /**
     * 处理心跳过期设备: 更新设备状态、发送设备故障通知邮件、分配维修人员维修
     *
     * @param deviceId 设备ID
     */
    public void dispose(String deviceId) {
        // 更新设备状态(获取乐观锁)
        Device device = deviceService.findByDeviceId(Integer.parseInt(deviceId));
        if (device == null) {
            return;
        }

        deviceService.updateDeviceStatus(
                device.setStatus(DeviceStatus.OFFLINE),
                true
        );

        User user = userService.findById(device.getUserId());
        String title = deviceId + "故障通知";
        String content = "尊敬的用户,系统检测出您的设备" + deviceId + "出现故障。" + "请您耐心等待,系统即将为您分配维修人员进行维修。";

        // 发送设备故障通知
        Message message = new Message()
                .setUserId(user.getUserId())
                .setTitle(title)
                .setType(MessageType.ERROR)
                .setContent(content);
        messageService.saveMessage(message);

        // 发送设备故障邮件
        emailService.sendMessage(user.getEmail(), content, title);

        // 尝试分配维修人员进行维修
        deviceService.doMaintenance(Integer.valueOf(deviceId));
    }

}
