package com.dreamfish.backend.controller;

import com.dreamfish.backend.common.ErrorCode;
import com.dreamfish.backend.common.Result;
import com.dreamfish.backend.entity.CustomUser;
import com.dreamfish.backend.entity.Worker;
import com.dreamfish.backend.entity.status.Gender;
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

/**
 * @author Dream fish
 * @version 1.0
 * @description: 维修人员控制类
 * @date 2025/4/11 15:15
 */
@RestController
@RequestMapping("/app/worker")
@Slf4j
@Tag(
        name = "维修人员操作",
        description = "获取当前用户的维修人员列表、维修人员信息、添加维修人员、删除维修人员、修改维修人员信息"
)
@SecurityRequirement(name = "BearerAuth")
@RequiredArgsConstructor
public class WorkerController {

    private final WorkerService workerService;

    @Operation(
            summary = "获取维修人员列表(分页)",
            description = "获取维修人员列表,需要 Token。",
            parameters = {
                    @Parameter(
                            name = "pageNum",
                            description = "页码"
                    ),
                    @Parameter(
                            name = "pageSize",
                            description = "页数"
                    ),
                    @Parameter(
                            name = "name",
                            description = "姓名"
                    ),
                    @Parameter(
                            name = "gender",
                            description = "性别"
                    )
            }
    )
    @GetMapping("/list")
    public Result<?> list(
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "9") int pageSize,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) Integer gender,
            @AuthenticationPrincipal CustomUser customUser
    ) {
        //将性别转换为枚举类型
        Gender s = null;
        if (gender != null) {
            s = Gender.fromCode(gender);
        }
        log.info("获取全部维修人员列表:name = {}, customUser = {}, status = {}", name, customUser, gender);
        return Result.success(workerService.findAllWorkers(pageNum, pageSize, new Worker().setUserId(customUser.getUserId()).setName(name).setGender(s)));
    }

    @Operation(
            summary = "获取维修人员信息",
            description = "获取维修人员信息,需要 Token。",
            parameters = {
                    @Parameter(
                            name = "workerId",
                            description = "维修人员ID",
                            required = true
                    )
            }
    )
    @GetMapping("/info")
    public Result<?> info(
            Worker worker
    ) {
        log.info("获取单个维修人员信息: {}", worker);
        if (worker.getWorkerId() == null) {
            return Result.error(HttpStatus.BAD_REQUEST, ErrorCode.PARAMETER_ILLEGAL, "维修人员ID不能为空");
        }

        return Result.success(workerService.findById(worker.getWorkerId()));
    }


    @Operation(
            summary = "删除维修人员",
            description = "删除维修人员,需要 Token。",
            parameters = {
                    @Parameter(
                            name = "workerId",
                            description = "维修人员ID",
                            required = true
                    )
            }
    )
    @GetMapping("/delete")
    public Result<?> delete(
            Worker worker
    ) {
        log.info("删除维修人员: {}", worker);

        if (worker.getWorkerId() == null) {
            return Result.error(HttpStatus.BAD_REQUEST, ErrorCode.PARAMETER_ILLEGAL, "维修人员ID不能为空");
        }

        workerService.deleteWorker(worker.getWorkerId());

        return Result.success();
    }

    @Operation(
            summary = "添加维修人员",
            description = "添加维修人员,需要 Token。",
            parameters = {
                    @Parameter(
                            name = "name",
                            description = "维修人员姓名",
                            required = true
                    ),
                    @Parameter(
                            name = "gender",
                            description = "维修人员性别",
                            required = true
                    ),
                    @Parameter(
                            name = "email",
                            description = "维修人员邮箱",
                            required = true
                    )
            }
    )
    @PostMapping("/add")
    public Result<?> add(
            @Validated @RequestBody Worker worker,
            @AuthenticationPrincipal CustomUser customUser
    ) {
        // JSR 303 校验

        log.info("添加维修人员: {}", worker);

        if (worker.getStatus() == null) {
            return Result.error(HttpStatus.BAD_REQUEST, ErrorCode.PARAMETER_ILLEGAL, "维修人员状态不能为空");
        }
        worker.setUserId(customUser.getUserId());
        workerService.register(worker);
        return Result.success();
    }

    @Operation(
            summary = "修改维修人员信息",
            description = "修改维修人员信息,需要 Token。",
            parameters = {
                    @Parameter(
                            name = "workerId",
                            description = "维修人员ID",
                            required = true
                    ),
                    @Parameter(
                            name = "name",
                            description = "维修人员姓名",
                            required = true
                    ),
                    @Parameter(
                            name = "email",
                            description = "维修人员邮箱",
                            required = true
                    )
            }
    )
    @PostMapping("/update")
    public Result<?> update(
            @Validated @RequestBody Worker worker
    ) {
        // JSR 303 校验
        log.info("更新维修人员信息: {}", worker);
        if (worker.getWorkerId() == null) {
            return Result.error(HttpStatus.BAD_REQUEST, ErrorCode.PARAMETER_ILLEGAL, "维修人员ID不能为空");
        }
        workerService.updateWorker(worker);
        return Result.success();
    }
}
