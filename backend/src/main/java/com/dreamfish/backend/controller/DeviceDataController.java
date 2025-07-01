package com.dreamfish.backend.controller;

import com.dreamfish.backend.common.ErrorCode;
import com.dreamfish.backend.common.RedisKeyPrefix;
import com.dreamfish.backend.common.Result;
import com.dreamfish.backend.component.DeviceHeartbeatDispose;
import com.dreamfish.backend.entity.Device;
import com.dreamfish.backend.entity.GeoPosition;
import com.dreamfish.backend.entity.Message;
import com.dreamfish.backend.entity.User;
import com.dreamfish.backend.entity.status.DeviceStatus;
import com.dreamfish.backend.entity.status.MessageType;
import com.dreamfish.backend.exception.DeviceException;
import com.dreamfish.backend.exception.MapServiceException;
import com.dreamfish.backend.exception.ParameterValidationException;
import com.dreamfish.backend.parser.annotation.BinaryRequestBody;
import com.dreamfish.backend.parser.group.Maintenance;
import com.dreamfish.backend.parser.group.Receive;
import com.dreamfish.backend.parser.group.Register;
import com.dreamfish.backend.service.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.concurrent.TimeUnit;

/**
 * @author Dream fish
 * @version 1.0
 * @description: 设备数据接收控制器:接收参数为字节流
 * @date 2025/4/3 14:53
 */
@RestController
@RequestMapping("/data/device")
@Tag(
        name = "设备数据接收",
        description = "设备数据接收控制器:接收参数为字节流(采用其它方式测试)(前期不进行设备验证)"
)
@Slf4j
@RequiredArgsConstructor
public class DeviceDataController {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    private final RedisTemplate<String, Object> redisTemplate;

    private final DeviceService deviceService;

    private final PasswordEncoder passwordEncoder;

    private final DeviceHeartbeatDispose deviceHeartbeatDispose;

    private final EmailService emailService;

    private final MessageService messageService;

    private final UserService userService;

    private final MapService mapService;

    @Operation(
            summary = "设备数据接收",
            description = "接收设备数据->存储到Kafka->消费者读取数据->验证设备合法性->存储到influxDB"
    )
    @PostMapping("/send")
    public Result<?> receiveData(
            @Validated @BinaryRequestBody(byteOrder = BinaryRequestBody.ByteOrder.BIG_ENDIAN, group = Receive.class) Device device
    ) {
        log.info("DeviceDataController接收到设备发送的数据: {}", device);

        //设置一下接收时间
        long timestampNanos = TimeUnit.MILLISECONDS.toNanos(System.currentTimeMillis());
        device.setReceiveTime(timestampNanos);

        // 立即更新设备心跳
        deviceHeartbeatDispose.updateRedisHeartbeat(device.getDeviceId());

        // 不进行设备合法性验证，避免处理过程中出错导致设备数据丢失

        // 发送设备数据到Kafka
        kafkaTemplate.send("io-devices", device.getDeviceId() + "", device);

        return Result.success();
    }


    @Operation(
            summary = "设备注册",
            description = "感觉这里若是注册失败则给用户发送通知信息较好(未实现,可能需要对注册进行try-catch):用户手动提供设备ID注册设备->redis中存储临时表->设备发送注册数据->从临时表中获取用户ID,同时会验证设备ID->加密API密钥->存储到数据库->发送验证邮件->返回注册结果"
    )
    @PostMapping("/register")
    public Result<?> registerDevice(
            @Validated @BinaryRequestBody(byteOrder = BinaryRequestBody.ByteOrder.BIG_ENDIAN, group = Register.class) Device device
    ) {
        log.info("DeviceDataController接收到注册设备: {}", device);

        // 公共校验部分(id、apikey)使用jsr303,其他部分手动校验
        GeoPosition geoPosition = device.getGeoPosition();
        if (geoPosition == null || geoPosition.getLatitude() == null || geoPosition.getLongitude() == null) {
            log.info("注册设备:经纬度为空");
            throw new ParameterValidationException(ErrorCode.PARAMETER_ILLEGAL, "经纬度不能为空");
        }

        // 先从临时表中获取用户ID,确保为用户手动操作
        String redisKey = RedisKeyPrefix.Device_Registration.generateFullKey(device.getDeviceId() + "");
        Device d = (Device) redisTemplate.opsForValue().get(
                redisKey
        );

        // 设置用户ID、设备描述
        if (d != null) {
            device.setUserId(d.getUserId());
            device.setDescription(d.getDescription());
        } else {
            log.info("注册设备:设备ID不存在");
            return Result.error(HttpStatus.BAD_REQUEST, ErrorCode.DEVICE_REGISTER_ILLEGAL);
        }

        // 判断设备ID是否已经注册过
        String title;
        String content;
        MessageType type;
        Device record = deviceService.findDeviceRecord(device.getDeviceId());
        if (record != null && record.getIsDeleted() == 0) {
            log.info("注册设备:设备ID已存在");
            title = device.getDeviceId() + "添加失败";
            content = "尊敬的用户,您的设备:" + device.getDeviceId() + "已经存在," + "若要继续添加请删除原设备后重新添加";
            type = MessageType.ERROR;
        } else {
            // apikey加密
            device.setApiKey(passwordEncoder.encode(device.getApiKey()));

            // 调用地图服务获取地址信息
            try {
                MapService.AddressInfo addressInfo = mapService.getAddressByCoordinate(
                        geoPosition.getLongitude().doubleValue(),
                        geoPosition.getLatitude().doubleValue()
                );

                // 设置所在省份和完整地址
                String city = addressInfo.addressComponent().getProvince();
                device.setCity(city);
                device.setFullAddress(addressInfo.fullAddress());

            } catch (MapServiceException e) {
                log.error("获取地理位置失败: {}", e.getMessage());

                title = device.getDeviceId() + "添加失败";
                content = "尊敬的用户,您的设备:" + device.getDeviceId() + "获取地理位置定位失败,请稍后再试!";
                type = MessageType.ERROR;
                notice(
                        title,
                        content,
                        type,
                        d.getUserId()
                );

                throw new DeviceException(ErrorCode.MAP_SERVICE_ERROR, "地图服务异常，无法解析地理位置!");
            }

            // 注册
            if (record == null) {
                // 设备记录不存在，进行注册
                deviceService.register(device);
            } else {
                // 设备记录存在，进行重新注册
                deviceService.reenrollDevice(device);
            }

            // 删除临时表中记录
            redisTemplate.delete(redisKey);

            title = device.getDeviceId() + "添加成功";
            content = "尊敬的用户,您的设备:" + device.getDeviceId() + "已经成功添加," + "详细信息请到后台查看";
            type = MessageType.INFO;

        }
        // 通知用户
        notice(
                title,
                content,
                type,
                d.getUserId()
        );

        // 返回结果
        return Result.success();
    }

    @Operation(
            summary = "设备维护检验",
            description = "worker 收到系统邮件等提醒->维修设备->设备提交维修成功请求并检验连接-> 更新设备状态"
    )
    @PostMapping("/maintenance")
    public Result<?> maintenanceDevice(
            @Validated @BinaryRequestBody(byteOrder = BinaryRequestBody.ByteOrder.BIG_ENDIAN, group = Maintenance.class) Device device
    ) {
        //参数校验交个JSR303

        log.info("DeviceDataController接收到维护设备: {}", device);

        // 验证apikey
        Device d = deviceService.findByDeviceId(device.getDeviceId());
        if (d == null) {
            log.info("维护设备:设备ID不存在");
            throw new DeviceException(ErrorCode.DEVICE_NOT_FOUND);
        }
        if (!passwordEncoder.matches(device.getApiKey(), d.getApiKey())) {
            log.info("维护设备:API密钥不匹配");
            throw new DeviceException(ErrorCode.DEVICE_INVALID);
        }

        // 更新设备状态
        deviceService.updateDeviceStatus(device.setStatus(DeviceStatus.NORMAL), true);

        // 通知用户
        String title = d.getDeviceId() + "维修通成功";
        String content = "尊敬的用户,您的设备:" + d.getDeviceId() + "已经成功维修," + "详细信息请到后台查看";
        notice(
                title,
                content,
                MessageType.INFO,
                d.getUserId()
        );

        // 返回结果,说明设备正常且连接正常
        return Result.success();
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
        Message message = new Message().
                setUserId(userId)
                .setContent(content)
                .setTitle(title)
                .setType(type);
        messageService.saveMessage(message);

        // 向用户发送邮件通知
        User user = userService.findById(userId);
        emailService.sendMessage(user.getEmail(), content, title);
    }
}