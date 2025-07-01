package com.dreamfish.backend.controller;

import com.dreamfish.backend.common.ErrorCode;
import com.dreamfish.backend.common.Result;
import com.dreamfish.backend.entity.CustomUser;
import com.dreamfish.backend.entity.MaintenanceRecord;
import com.dreamfish.backend.entity.status.MaintenanceRecordStatus;
import com.dreamfish.backend.service.MaintenanceRecordService;
import com.dreamfish.backend.service.WorkerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.sql.Date;

/**
 * @author Dream fish
 * @version 1.0
 * @description: 维修记录
 * @date 2025/4/12 18:20
 */
@RestController
@RequestMapping("/app/maintenanceRecords")
@Slf4j
@Tag(
        name = "维修记录",
        description = "获取维修记录分页列表、获取维修记录详情、上传维修记录"
)
@RequiredArgsConstructor
public class MaintenanceRecordsController {
    private final MaintenanceRecordService maintenanceRecordService;
    private final WorkerService workerService;

    @Operation(
            summary = "获取维修记录详情",
            description = "获取维修记录详情,需要 Token。用户上传维修记录前获取部分信息。",
            parameters = {
                    @Parameter(
                            name = "id",
                            description = "维修记录ID",
                            required = true
                    )
            }
    )
    @GetMapping("/detail")
    public Result<?> getMaintenanceRecordDetail(
            Integer id
    ) {
        // 判断当前维修记录是否已经完成
        MaintenanceRecord record = maintenanceRecordService.findMaintenanceRecordById(id);
        if (record.getStatus() != MaintenanceRecordStatus.UNFINISHED) {
            // 返回空,前端自动重定向404
            return Result.success(null);
        }

        // 返回维修记录
        return Result.success(maintenanceRecordService.findMaintenanceRecordById(id));
    }

    @Operation(
            summary = "上传维修记录",
            description = "上传维修记录,需要 Token。",
            parameters = {
                    @Parameter(
                            name = "id",
                            description = "维修记录ID",
                            required = true
                    ),
                    @Parameter(
                            name = "description",
                            description = "故障描述",
                            required = true
                    ),
                    @Parameter(
                            name = "process",
                            description = "维修过程",
                            required = true
                    )
            }
    )
    @PostMapping("/push")
    public Result<?> pushMaintenanceRecords(
            @Validated @RequestBody MaintenanceRecord maintenanceRecord
    ) {
        // 其他校验交给 JSR303 处理
        if (maintenanceRecord.getId() == null) {
            return Result.error(HttpStatus.BAD_REQUEST, ErrorCode.PARAMETER_ILLEGAL, "维修记录ID不能为空");
        }

        // 上传维修记录
        maintenanceRecordService.updateMaintenanceRecords(
                maintenanceRecord
                        .setFinishedAt(new java.util.Date(System.currentTimeMillis()))
                        .setStatus(MaintenanceRecordStatus.FINISHED)
        );

        // 此时认为维修人员已经完成维修,释放维修人员
        MaintenanceRecord record = maintenanceRecordService.findMaintenanceRecordById(maintenanceRecord.getId());

        log.info("维修人员 {} 已经完成维修", record.getWorker().getName());

        // 释放维修人员
        workerService.releaseWorker(record.getWorker().getWorkerId());

        return Result.success();
    }

    @Operation(
            summary = "获取维修记录分页列表",
            description = "获取维修记录分页列表,需要 Token。",
            parameters = {
                    @Parameter(
                            name = "pageNum",
                            description = "页码"
                    ),
                    @Parameter(
                            name = "pageSize",
                            description = "每页记录数"
                    ),
                    @Parameter(
                            name = "search",
                            description = "搜索关键词"
                    ),
                    @Parameter(
                            name = "status",
                            description = "状态"
                    ),
                    @Parameter(
                            name = "startTime",
                            description = "开始时间"
                    ),
                    @Parameter(
                            name = "endTime",
                            description = "结束时间"
                    )
            }
    )
    @SecurityRequirement(name = "BearerAuth")
    @GetMapping("/list")
    public Result<?> list(
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "9") int pageSize,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) Date startTime,
            @RequestParam(required = false) Date endTime,
            @AuthenticationPrincipal CustomUser customUser
    ) {

        // 将状态转换为枚举类型
        MaintenanceRecordStatus s = null;
        if (status != null) {
            s = MaintenanceRecordStatus.fromCode(status);
        }
        return Result.success(maintenanceRecordService.findAllMaintenanceRecords(
                pageNum,
                pageSize,
                customUser.getUserId(),
                search,
                s,
                startTime,
                endTime
        ));
    }

}
