package com.dreamfish.backend.service;

import com.dreamfish.backend.common.ErrorCode;
import com.dreamfish.backend.entity.Device;
import com.dreamfish.backend.entity.Message;
import com.dreamfish.backend.entity.User;
import com.dreamfish.backend.entity.status.DeviceStatus;
import com.dreamfish.backend.entity.status.MessageType;
import com.dreamfish.backend.exception.DeviceException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.annotation.RetryableTopic;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.retry.annotation.Backoff;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * @author Dream fish
 * @version 1.0
 * @description: kafka消费者
 * @date 2025/4/5 16:45
 */
//@Service
@Slf4j
@RequiredArgsConstructor
@Component
public class KafkaConsumerService {
    private final DeviceService deviceService;
    private final PasswordEncoder passwordEncoder;
    private final InfluxDBService influxDBService;
    private final MessageService messageService;
    private final UserService userService;
    private final EmailService emailService;
    @Value("${device.abnormal.threshold}")
    private Double threshold;

    /**
     * 消费数据,失败自动重试
     *
     * @param record kafka 消息记录
     * @param ack    手动提交偏移量
     */
    @RetryableTopic(
            attempts = "3",
            backoff = @Backoff(value = 2_000L),
            retryTopicSuffix = "-myRetry",
            dltTopicSuffix = "-myDlt"
    )
    @KafkaListener(topics = "io-devices", concurrency = "1", groupId = "iot-group", autoStartup = "${kafka.auto.start:true}")
    public void consume(ConsumerRecord<String, Device> record, Acknowledgment ack) {
        log.info("kafka消费者接收到消息: {}", record.value());
        try {
            Device device = record.value();
            processMessage(device);
            ack.acknowledge();
        } catch (Exception e) {
            log.error("处理失败，暂停提交偏移量", e);
            throw new DeviceException(ErrorCode.DB_SAVE_CODE, "数据处理异常");
        }
    }

    /**
     * 数据处理
     *
     * @param device 设备信息
     */
    private void processMessage(Device device) {
        try {
            // 参数和设备合法性验证
            if (!validateDevice(device)) {
                return;
            }

            //验证设备合法性
            Device d = deviceService.findByDeviceId(device.getDeviceId());
            if (d == null) {
                log.error("processMessage:设备不存在 {}", device.getDeviceId());
                return;
            }

            // 验证API密钥并存储数据内容
            if (passwordEncoder.matches(device.getApiKey(), d.getApiKey())) {
                log.info("设备验证成功: {}", device.getDeviceId());
                influxDBService.save(device);
            } else {
                log.error("设备验证失败: {}", device.getDeviceId());
            }

            // 判断设备数据是否异常
            if (device.getResistance() >= threshold && d.getStatus() != DeviceStatus.ABNORMAL) {
                log.error("设备数据异常: {}", device.getDeviceId());

                // 通知用户
                String title = d.getDeviceId() + "设备数据异常";
                String content = "尊敬的用户，您的设备:" + d.getDeviceId() + "数据出现异常(" + device.getResistance() + "),请留意!";
                notice(title, content, MessageType.WARNING, d.getUserId());

                // 更新设备状态
                deviceService.updateDeviceStatus(d.setStatus(DeviceStatus.ABNORMAL), true);
            } else {
                log.info("设备数据正常: {}", device.getDeviceId());

                //更新设备状态
                if (d.getStatus() == DeviceStatus.OFFLINE) {
                    deviceService.updateDeviceStatus(d.setStatus(DeviceStatus.NORMAL), true);
                }
            }
        } catch (Exception e) {
            log.error("数据处理异常: {}", e.getMessage());
            // 触发kafka重试机制
            throw new DeviceException(ErrorCode.DB_SAVE_CODE, "数据处理异常");
        }
    }

    /**
     * 设备参数验证
     *
     * @param device 设备信息
     * @return 验证结果
     */
    private boolean validateDevice(Device device) {
        // 设备ID和API密钥不能为空
        if (device.getDeviceId() == null) {
            log.error("processMessage:设备ID为空");
            return false;
        }
        if (device.getApiKey() == null) {
            log.error("processMessage:设备API密钥为空");
            return false;
        }
        if (device.getResistance() == null) {
            log.error("processMessage:设备电阻值为空");
            return false;
        }
        return true;
    }

    /**
     * 创建系统通知和发送邮件通知
     *
     * @param title   通知标题
     * @param content 通知内容
     * @param type    通知类型
     * @param userId  用户id
     */
    private void notice(String title, String content, MessageType type, Integer userId) {
        // 添加消息通知
        Message message = new Message()
                .setUserId(userId)
                .setContent(content)
                .setTitle(title)
                .setType(type);
        messageService.saveMessage(message);

        // 向用户发送邮件通知
        User user = userService.findById(userId);
        emailService.sendMessage(user.getEmail(), content, title);
    }

}