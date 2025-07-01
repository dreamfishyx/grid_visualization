package com.dreamfish.backend.component;

import com.dreamfish.backend.entity.User;
import com.dreamfish.backend.event.WorkerReleasedEvent;
import com.dreamfish.backend.service.DeviceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * @author Dream fish
 * @version 1.0
 * @description: 设备维修队列监听
 * @date 2025/4/14 11:41
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MaintenanceQueueListener {
    private final DeviceService deviceService;

    /**
     * 处理监听到的维修工程师释放事件
     *
     * @param event 事件
     */
    @EventListener
    public void handleWorkerReleasedEvent(WorkerReleasedEvent event) {
        deviceService.doMaintenance(new User().setUserId(event.getUserId()));
    }
}
