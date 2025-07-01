package com.dreamfish.backend.controller;

import com.dreamfish.backend.common.RedisKeyPrefix;
import com.dreamfish.backend.common.Result;
import com.dreamfish.backend.component.VerificationCodeUtil;
import com.dreamfish.backend.entity.Device;
import com.dreamfish.backend.entity.User;
import com.dreamfish.backend.entity.status.DeviceStatus;
import com.dreamfish.backend.event.WorkerReleasedEvent;
import com.dreamfish.backend.service.DeviceService;
import com.dreamfish.backend.service.EmailService;
import com.dreamfish.backend.service.InfluxDBService;
import com.dreamfish.backend.service.MapService;
import io.swagger.v3.oas.annotations.Hidden;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * @author Dream fish
 * @version 1.0
 * @description: 测试
 * @date 2025/4/11 14:39
 */
@RestController
@RequestMapping("/test")
@Hidden
@RequiredArgsConstructor
@Slf4j
public class TestController {
    private final VerificationCodeUtil codeUtil;
    private final EmailService emailService;
    private final TemplateEngine templateEngine;
    private final MapService mapService;
    private final DeviceService deviceService;
    private final InfluxDBService influxDBService;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final RedisTemplate<String, Object> redisTemplate;
    private final ApplicationEventPublisher eventPublisher;


    @GetMapping("/send-code")
    public Result<?> sendCode(User user) {

        String email = user.getEmail();

        // 生成6位验证码
        String code = codeUtil.generateCode(email);

        log.info("email: {}, code: {}", email, code);

        // 发送验证码
        emailService.sendVerifyCode(email, code);

        return Result.success();
    }

    @GetMapping("/send-task")
    public Result<?> sendTask(User user) {

        // 北京
        MapService.AddressInfo addressInfo = mapService.getAddressByCoordinate(116.397477, 39.908692);

        Context context = new Context();
        context.setVariable("title", "维修任务通知");
        context.setVariable("workName", "长安客");
        context.setVariable("address", addressInfo.fullAddress());
        context.setVariable("navUrl", mapService.generateMarkerUrl(116.397477, 39.908692, "待维修设备"));
        context.setVariable("coordinates", String.format("经度: %.6f, 纬度: %.6f", 116.397477, 39.908692));
        context.setVariable("currentYear", LocalDate.now().getYear());

        String content = templateEngine.process("task—email-template.html", context);
        emailService.sendTask(user.getEmail(), content);

        return Result.success();
    }


    @GetMapping("/test-device-update")
    public Result<?> testDeviceUpdate() {
        Device d = deviceService.findByDeviceId(2);
        deviceService.updateDeviceStatus(new Device().setDeviceId(1).setStatus(DeviceStatus.OFFLINE).setVersion(d.getVersion()), true);
        return Result.success();
    }

    @GetMapping("/data")
    public Result<?> testData() {
        Device test = new Device().setDeviceId(3).setDescription("test").setDeviceId(2);
        log.info("添加数据到卡夫卡: {}", test);
        kafkaTemplate.send("io-devices", test.getDeviceId() + "", test);
        return Result.success();
    }

    @GetMapping("/influxdb")
    public Result<?> testInfluxdb(
            Float resistance
    ) {
        long timestampNanos = TimeUnit.MILLISECONDS.toNanos(System.currentTimeMillis());
        Device test = new Device().setDeviceId(2).setResistance(resistance).setReceiveTime(timestampNanos);
        influxDBService.save(test);
        return Result.success();
    }

    @GetMapping("/influxdb-query")
    public Result<?> testInfluxdbQuery() {
        List<BigDecimal> list = influxDBService.analyseHourAgo(2);
        List<BigDecimal> list1 = influxDBService.analyseLast12Hours(3);
        HashMap<String, List<BigDecimal>> map = new HashMap<>();
        map.put("hour", list);
        map.put("day", list1);
        return Result.success(
                map
        );
    }

    @GetMapping("/heartbeat")
    public Result<?> testInfluxdbQuery2() {
        redisTemplate.opsForValue().set(RedisKeyPrefix.DEVICE_HEART_BEAT.generateFullKey("test"), "test", 10, TimeUnit.SECONDS);
        return Result.success();
    }

    @GetMapping("/release-worker-event")
    public Result<?> testReleaseWorker() {
        eventPublisher.publishEvent(new WorkerReleasedEvent(this, 2));
        return Result.success();
    }
}
