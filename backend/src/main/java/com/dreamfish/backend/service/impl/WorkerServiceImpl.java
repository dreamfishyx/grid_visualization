package com.dreamfish.backend.service.impl;

import com.dreamfish.backend.common.ErrorCode;
import com.dreamfish.backend.dao.DeviceMapper;
import com.dreamfish.backend.dao.WorkerMapper;
import com.dreamfish.backend.entity.*;
import com.dreamfish.backend.entity.status.MaintenanceRecordStatus;
import com.dreamfish.backend.entity.status.MessageType;
import com.dreamfish.backend.entity.status.WorkerStatus;
import com.dreamfish.backend.event.WorkerReleasedEvent;
import com.dreamfish.backend.exception.DbOperationFailedException;
import com.dreamfish.backend.exception.DeviceException;
import com.dreamfish.backend.exception.WorkerException;
import com.dreamfish.backend.service.*;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.time.LocalDate;
import java.util.List;

/**
 * @author Dream fish
 * @version 1.0
 * @description: worker service 实现类
 * @date 2025/4/11 15:38
 */
@Service
@RequiredArgsConstructor
public class WorkerServiceImpl implements WorkerService {

    private final WorkerMapper workerMapper;
    private final TemplateEngine templateEngine;
    private final EmailService emailService;
    private final MapService mapService;
    private final MessageService messageService;
    private final MaintenanceRecordService maintenanceRecordService;
    private final ApplicationEventPublisher eventPublisher;
    private final DeviceMapper deviceMapper;
    private final SSEService sseService;
    @Value("${maintenance.records.push.url}")
    private String pushUrl;


    @Override
    public void register(Worker worker) {
        // 添加维修人员
        workerMapper.insertWorker(worker);

        // 手动触发维修人员释放事件
        eventPublisher.publishEvent(new WorkerReleasedEvent(this, worker.getUserId()));
    }

    @Override
    public Worker findById(Integer workerId) {
        Worker worker = workerMapper.getWorker(workerId);
        if (worker == null) {
            throw new WorkerException(ErrorCode.WORKER_NOT_FIND);
        }
        return worker;
    }

    @Override
    @Transactional
    public void updateWorker(Worker worker) {
        // 获取乐观锁:防止更新期间被删除
        Worker w = workerMapper.getWorker(worker.getWorkerId());

        // 更新维修人员信息
        int i = workerMapper.updateWorkerInfo(worker.setVersion(w.getVersion()));
        if (i == 0) {
            throw new DeviceException(ErrorCode.DB_OPERATION_FAILED, "当前维修人员被占用,无法操作!");
        }

        //提醒前端更新数据
        sendUpdateMessage(worker.getWorkerId());
    }

    @Override
    @Transactional
    public void deleteWorker(Integer workerId) {
        // 获取乐观锁
        Worker w = workerMapper.getWorker(workerId);

        // 判断当前维修人员是否空闲
        if (w.getStatus() != WorkerStatus.FREE) {
            throw new DeviceException(ErrorCode.DB_OPERATION_FAILED, "当前维修人员不是空闲状态,无法删除!");
        }

        // 尝试删除
        int i = workerMapper.deleteWorker(w);
        if (i == 0) {
            throw new DeviceException(ErrorCode.DB_OPERATION_FAILED, "当前维修人员被占用,无法删除!");
        }

        // 通知前端更新
        sendUpdateMessage(w.getUserId());
    }

    @Override
    public PageInfo<Worker> findAllWorkers(Integer pageNum, Integer pageSize, Worker worker) {
        PageHelper.startPage(pageNum, pageSize);
        List<Worker> workers = workerMapper.getWorkers(worker);
        return new PageInfo<>(workers);
    }


    @Override
    @Transactional
    public Worker assignWorker() throws DbOperationFailedException {
        // 尝试获取一个维修人员,并获取乐观锁
        Worker worker = workerMapper.getFirstFreeWorker();

        if (worker != null) {
            // 更新维修人员状态
            int i = workerMapper.updateWorkerStatus(worker.setStatus(WorkerStatus.BUSY));

            if (i == 0) {
                throw new DeviceException(ErrorCode.DB_OPERATION_FAILED, "当前维修人员被占用,无法执行操作!");
            }else{
                // sendUpdateMessage(worker.getUserId());
                // 在事务提交后触发 SSE 通知
                TransactionSynchronizationManager.registerSynchronization(
                        new TransactionSynchronization() {
                            @Override
                            public void afterCommit() {
                                sendUpdateMessage(worker.getUserId());
                            }
                        }
                );
            }

            return worker;
        }

        // 通知前端更新数据

        return null;
    }

    @Override
    @Transactional
    public void releaseWorker(Integer workerId) {

        // 此时员工不会被删除
        updateWorkerStatus(new Worker().setWorkerId(workerId).setStatus(WorkerStatus.FREE));

        // 手动触发员工释放事件
        eventPublisher.publishEvent(new WorkerReleasedEvent(this, getUserIdByDeviceId(workerId)));

        // 通知前端
        sendUpdateMessage(getUserIdByDeviceId(workerId));
    }

    @Override
    @Transactional
    public void assignTask(Worker worker, Device device) {
        // 创建一条维修记录,返回维修记录的 id
        MaintenanceRecord records = new MaintenanceRecord()
                .setDeviceId(device.getDeviceId())
                .setUserId(worker.getUserId())
                .setStatus(MaintenanceRecordStatus.UNFINISHED)
                .setWorker(worker);
        maintenanceRecordService.insertMaintenanceRecords(records);
        Integer id = records.getId();

        // 拼接一个维修记录上传操作的 url
        String url = String.format("%s/%s", pushUrl, id);

        // 向维修人员发送邮件等等,并创建一个维修记录
        GeoPosition geo = device.getGeoPosition();
        Context context = new Context();
        context.setVariable("title", "设备维修通知");
        context.setVariable("workName", worker.getName());
        context.setVariable("address", device.getFullAddress());
        context.setVariable("navUrl", mapService.generateMarkerUrl(geo.getLongitude().doubleValue(), geo.getLatitude().doubleValue(), "待维修设备"));
        context.setVariable("coordinates", String.format("经度: %.6f, 纬度: %.6f", geo.getLongitude().doubleValue(), geo.getLatitude().doubleValue()));
        context.setVariable("resultPushUrl", url);
        context.setVariable("currentYear", LocalDate.now().getYear());

        // 发送邮件
        emailService.sendTask(
                worker.getEmail(),
                templateEngine.process("task—email-template.html", context)
        );

        // 添加用户系统通知
        String title = device.getDeviceId() + "开始维修";
        String content = "尊敬的用户,您的设备:" + device.getDeviceId() + "已经分配维修人员维修" + "详细信息联系维修人员:" + worker.getEmail();
        Message message = new Message().
                setUserId(device.getUserId())
                .setContent(content)
                .setTitle(title).setType(MessageType.INFO);

        messageService.saveMessage(message);

        // 添加用户邮件通知:没必要
    }

    @Override
    public Integer getUserIdByDeviceId(Integer deviceId) {
        Device device = deviceMapper.findByDeviceId(deviceId);
        if (device == null) {
            throw new DeviceException(ErrorCode.DEVICE_NOT_FOUND);
        }
        return device.getUserId();
    }

    @Override
    @Transactional
    public void updateWorkerStatus(Worker worker) {
        //更新维修人员状态
        int i = workerMapper.updateWorkerStatus(worker);

        if (i == 0) {
            throw new DeviceException(ErrorCode.DB_OPERATION_FAILED, "当前维修人员被占用,无法执行操作!");
        }else{
            // 在事务提交后触发 SSE 通知
            TransactionSynchronizationManager.registerSynchronization(
                    new TransactionSynchronization() {
                        @Override
                        public void afterCommit() {
                            sendUpdateMessage(worker.getUserId());
                        }
                    }
            );
        }
    }

    /**
     * 通过 sse 发送维修人员更新信息
     *
     * @param userId 用户id
     */
    private void sendUpdateMessage(Integer userId) {
        // 提醒前端更新数据
        sseService.sendDataUpdateEvent(
                userId + "", "worker-update"
        );
    }
}
