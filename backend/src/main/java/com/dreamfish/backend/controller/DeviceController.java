package com.dreamfish.backend.controller;

import com.dreamfish.backend.common.ErrorCode;
import com.dreamfish.backend.common.RedisKeyPrefix;
import com.dreamfish.backend.common.Result;
import com.dreamfish.backend.entity.CustomUser;
import com.dreamfish.backend.entity.Device;
import com.dreamfish.backend.entity.status.DeviceStatus;
import com.dreamfish.backend.service.DeviceService;
import com.dreamfish.backend.service.InfluxDBService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.List;
import java.util.Objects;

/**
 * @author Dream fish
 * @version 1.0
 * @description: 设备控制类
 * @date 2025/4/11 14:50
 */
@Slf4j
@RestController
@Tag(
        name = "设备操作",
        description = "获取设备分页列表、设备详情、删除设备、添加设备、设备列表等"
)
@SecurityRequirement(name = "BearerAuth")
@RequestMapping("/app/device")
@RequiredArgsConstructor
public class DeviceController {
    private final DeviceService deviceService;
    private final InfluxDBService influxDBService;
    private final RedisTemplate<String, Object> redisTemplate;

    @Operation(
            summary = "获取设备列表(分页数据)",
            description = "获取设备列表,需要 Token。",
            parameters = {
                    @Parameter(
                            name = "page",
                            description = "页码"
                    ),
                    @Parameter(
                            name = "size",
                            description = "每页数量"
                    ),
                    @Parameter(
                            name = "search",
                            description = "搜索关键字"
                    ),
                    @Parameter(
                            name = "status",
                            description = "设备状态"
                    )
            }
    )
    @GetMapping("/list-page")
    public Result<?> getDevices(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "9") int size,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Integer status,
            @AuthenticationPrincipal CustomUser customUser
    ) {
        DeviceStatus code = null;
        if (status != null) {
            code = DeviceStatus.fromCode(status);
        }
        return Result.success(deviceService.getDevicesByPage(page, size, customUser.getUserId(), search, code));
    }

    @Operation(
            summary = "获取设备列表(不分页,全部数据)",
            description = "获取设备列表,需要 Token。",
            parameters = {
                    @Parameter(
                            name = "search",
                            description = "搜索关键字"
                    ),
                    @Parameter(
                            name = "status",
                            description = "设备状态"
                    )
            }
    )
    @GetMapping("/list")
    public Result<?> getAllDevices(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Integer status,
            @AuthenticationPrincipal CustomUser customUser
    ) {
        DeviceStatus code = null;
        if (status != null) {
            code = DeviceStatus.fromCode(status);
        }
        return Result.success(deviceService.getDevices(customUser.getUserId(), search, code));
    }

    @Operation(
            summary = "获取指定设备信息",
            description = "获取指定设备信息,需要 Token。",
            parameters = {
                    @Parameter(
                            name = "deviceId",
                            description = "设备ID",
                            required = true
                    )
            }
    )
    @GetMapping("/info")
    public Result<?> deviceInfo(
            Integer deviceId
    ) {
        Device device = deviceService.findByDeviceId(deviceId);
        if (device == null) {
            return Result.error(HttpStatus.BAD_REQUEST, ErrorCode.DEVICE_NOT_FOUND, "设备不存在");
        }

        return Result.success(
                device
        );
    }


    @Operation(
            summary = "查询指定设备过去的数据",
            description = "查询指定设备过去的数据,需要 Token。",
            parameters = {
                    @Parameter(
                            name = "deviceId",
                            description = "设备ID",
                            required = true
                    ),
                    @Parameter(
                            name = "mod",
                            description = "查询数据的粒度:hour/half-day",
                            required = true
                    )
            }
    )
    @GetMapping("/query-data")
    public Result<?> deviceQuery(Integer deviceId, String mod) {
        List<BigDecimal> list;
        if ("hour".equals(mod)) {
            list = influxDBService.analyseHourAgo(deviceId);
        } else {
            list = influxDBService.analyseLast12Hours(deviceId);
        }
        log.info("查询结果:mod:{}, list:{}", mod, list);
        return Result.success(list);
    }


    @Operation(
            summary = "预注册设备",
            description = "添加设备,需要 Token。会将设备信息 临时存储在redis中,等待物联网设备提交注册请求。",
            parameters = {
                    @Parameter(
                            name = "deviceId",
                            description = "设备ID",
                            required = true
                    ),
                    @Parameter(
                            name = "description",
                            description = "设备描述",
                            required = true
                    )
            }
    )
    @PostMapping("/add")
    public Result<?> deviceAdd(
            @RequestBody Device device,
            @AuthenticationPrincipal CustomUser customUser
    ) {
        if (device.getDeviceId() == null) {
            return Result.error(HttpStatus.BAD_REQUEST, ErrorCode.PARAMETER_ILLEGAL, "设备ID不能为空");
        }

        if (device.getDescription() == null) {
            return Result.error(HttpStatus.BAD_REQUEST, ErrorCode.PARAMETER_ILLEGAL, "设备描述不能为空");
        }

        Device record = deviceService.findDeviceRecord(device.getDeviceId());
        if (record != null && record.getIsDeleted() == 0) {
            return Result.error(HttpStatus.BAD_REQUEST, ErrorCode.PARAMETER_ILLEGAL, "设备已存在");
        }

        // 存入redis零时表中等待设备提交注册请求
        redisTemplate.opsForValue().set(RedisKeyPrefix.Device_Registration.generateFullKey(device.getDeviceId() + ""), device.setUserId(customUser.getUserId()), Duration.ofHours(2));
        return Result.success();
    }

    @Operation(
            summary = "删除设备",
            description = "删除设备,需要 Token。",
            parameters = {
                    @Parameter(
                            name = "deviceId",
                            description = "设备ID",
                            required = true
                    )
            }
    )
    @GetMapping("/delete")
    public Result<?> deviceDelete(
            Integer deviceId,
            @AuthenticationPrincipal CustomUser customUser

    ) {
        // 参数校验
        if (deviceId == null) {
            return Result.error(HttpStatus.BAD_REQUEST, ErrorCode.PARAMETER_ILLEGAL, "设备ID不能为空");
        }

        // 判断当前设备是否属于当前用户
        Device d = deviceService.findByDeviceId(deviceId);
        if (d == null || !Objects.equals(d.getUserId(), customUser.getUserId())) {
            return Result.error(HttpStatus.BAD_REQUEST, ErrorCode.DEVICE_NOT_FOUND, "当前设备不属于当前用户");
        }

        // 删除
        deviceService.deleteDevice(deviceId);

        return Result.success();
    }
}
