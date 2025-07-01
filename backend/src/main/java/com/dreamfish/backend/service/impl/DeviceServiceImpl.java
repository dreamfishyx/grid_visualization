package com.dreamfish.backend.service.impl;

import com.dreamfish.backend.common.ErrorCode;
import com.dreamfish.backend.common.RedisKeyPrefix;
import com.dreamfish.backend.dao.DeviceMapper;
import com.dreamfish.backend.entity.Device;
import com.dreamfish.backend.entity.User;
import com.dreamfish.backend.entity.Worker;
import com.dreamfish.backend.entity.status.DeviceStatus;
import com.dreamfish.backend.exception.DbOperationFailedException;
import com.dreamfish.backend.service.DeviceService;
import com.dreamfish.backend.service.MaintenanceRecordService;
import com.dreamfish.backend.service.SSEService;
import com.dreamfish.backend.service.WorkerService;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.List;

/**
 * @author Dream fish
 * @version 1.0
 * @description: 设备服务
 * @date 2025/4/3 21:19
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class DeviceServiceImpl implements DeviceService {
    private final DeviceMapper deviceMapper;
    private final RedisTemplate<String, Object> redisTemplate;
    private final WorkerService workerService;
    private final SSEService sseService;
    private final MaintenanceRecordService maintenanceRecordService;


    @Override
    public Device findByDeviceId(Integer deviceId) {

        // 尝试从缓存获取
        Device device = (Device) redisTemplate.opsForValue().get(RedisKeyPrefix.DEVICE_CACHE.generateFullKey(deviceId + ""));
        if (device != null) {
            log.info("缓存命中,获取到数据:{}", device);
            return device;
        }

        // 缓存未命中，从数据库中查询
        device = deviceMapper.findByDeviceId(deviceId);

        // 将设备信息保存到缓存
        redisTemplate.opsForValue().set(RedisKeyPrefix.DEVICE_CACHE.generateFullKey(deviceId + ""), device);

        return device;
    }

    @Override
    public Device findDeviceRecord(Integer deviceId) {
        return deviceMapper.findDeviceRecord(deviceId);
    }

    @Override
    public void register(Device device) {

        // 判断当前设备是否已经存在
        // if (existsByDeviceId(device.getDeviceId())) {
        // throw new DeviceException(ErrorCode.DB_OPERATION_FAILED, "设备已存在,请删除后重新添加!");
        // }

        // 将设备信息保存到数据库
        deviceMapper.insertDevice(device);

        // 提醒前端更新数据
        sendUpdateMessage(device.getUserId());
    }

    @Override
    public void reenrollDevice(Device device) {
        // 将设备信息保存到数据库
        deviceMapper.reenrollDevice(device);

        // 提醒前端更新数据
        sendUpdateMessage(device.getUserId());
    }

    @Override
    @Transactional
    public void updateDeviceStatus(Device device, Boolean sendMessage) {

        // 更新设备状态
        int i = deviceMapper.updateDeviceStatus(device);
        if (i == 0) {
            throw new DbOperationFailedException(ErrorCode.DB_OPERATION_FAILED, "更新设备状态失败");
        }

        // 获取设备所属用户
        Integer userId = device.getUserId();
        if (userId == null) {
            device.setUserId(
                    findByDeviceId(device.getDeviceId()).getUserId()
            );
        }

        // 提醒前端更新数据
        if (sendMessage) {
            // 在事务提交后触发 SSE 通知
            TransactionSynchronizationManager.registerSynchronization(
                    new TransactionSynchronization() {
                        @Override
                        public void afterCommit() {
                            sendUpdateMessage(device.getUserId());
                        }
                    }
            );
        }

        // 删除设备缓存
        redisTemplate.delete(RedisKeyPrefix.DEVICE_CACHE.generateFullKey(device.getDeviceId() + ""));
    }

    @Override
    public boolean existsByDeviceId(Integer deviceId) {
        return findByDeviceId(deviceId) != null;
    }

    @Override
    @Transactional
    public void doMaintenance(Integer deviceId) {
        //获取设备所属用户
        Integer uid = getUserIdByDeviceId(deviceId);
        if (uid == null) {
            return;
        }

        // 获取设备乐观锁
        Device device = findByDeviceId(deviceId);
        if (device == null) {
            // 设备在此期间被误删
            return;
        }

        // 尝试分配一个维修人员
        Worker worker = workerService.assignWorker();
        if (worker == null) {
            // 将设备加入维修队列
            redisTemplate.opsForList().leftPush(
                    RedisKeyPrefix.MAINTENANCE_QUEUE.generateFullKey(uid + ""),
                    deviceId
            );
        } else {
            // 将设备分配给维修人员
            workerService.assignTask(worker, findByDeviceId(deviceId));

            // 判断在此期间，是否有过删除行为有则报错回滚
            updateDeviceStatus(device, false);
        }

    }

    @Transactional
    @Override
    public void doMaintenance(User user) {
        // 获取用户id
        Integer uid = user.getUserId();
        if (uid == null) {
            return;
        }

        // 从维修队列中获取设备
        Integer deviceId = (Integer) redisTemplate.opsForList().rightPop(
                RedisKeyPrefix.MAINTENANCE_QUEUE.generateFullKey(uid + "")
        );
        if (deviceId == null) {
            //队列中不含待维修设备
            return;
        }

        // 通过乐观锁避免维修过程中设备被误删
        Device device = findByDeviceId(deviceId);
        if (device == null) {
            // 设备在等待维修期间被删除:重新开始获取设备
            doMaintenance(user);
            return;
        }

        // 分配一个 维修人员
        Worker worker = workerService.assignWorker();
        if (worker == null) {
            // 将设备重新加入维修队列
            redisTemplate.opsForList().leftPush(
                    RedisKeyPrefix.MAINTENANCE_QUEUE.generateFullKey(uid + ""),
                    deviceId
            );
            return;
        }

        // 将设备分配给维修人员
        workerService.assignTask(worker, findByDeviceId(deviceId));

        // 判断在此期间，是否有过删除行为有则报错回滚
        // redis不支持回滚,是否需要手动回滚?不需要设备都被删除(由于其他逻辑的保障,一定是删除)
        updateDeviceStatus(device, false);
    }


    @Override
    public Integer getUserIdByDeviceId(Integer deviceId) {
        Device device = findByDeviceId(deviceId);
        if (device == null) {
            return null;
        }
        return device.getUserId();
    }

    /**
     * 其实只要设备没有分配维修工人，那么设备就是可以删除的
     *
     * @param deviceId 设备id
     */
    @Override
    @Transactional
    public void deleteDevice(Integer deviceId) {

        // 判断设备是正在被占用
        if (maintenanceRecordService.isDeviceOccupancy(deviceId)) {
            throw new DbOperationFailedException(ErrorCode.DB_OPERATION_FAILED, "设备正在被占用,无法删除！");
        }

        // 获取乐观锁:开始尝试删除设备
        Device device = findByDeviceId(deviceId);
        if (device == null) {
            return;
        }

        // 由于 redis 不支持回滚
        // 将设备从维修队列中删除?不执行,维修的时候会判断设备是否存在
        // 删除设备心跳?不执行,心跳过期会判断设备是否存在

        // 删除设备
        Integer i = deviceMapper.deleteDevice(device);
        if (i == 0) {
            throw new DbOperationFailedException(ErrorCode.DB_OPERATION_FAILED, "设备正在被占用,暂时无法删除");
        } else {
            // sendUpdateMessage(getUserIdByDeviceId(device.getUserId()));
            // 在事务提交后触发 SSE 通知
            TransactionSynchronizationManager.registerSynchronization(
                    new TransactionSynchronization() {
                        @Override
                        public void afterCommit() {
                            sendUpdateMessage(getUserIdByDeviceId(device.getUserId()));
                        }
                    }
            );
        }

        // 删除设备相关维修记录
        maintenanceRecordService.deleteRecordWithDeviceId(deviceId);

        // 删除设备缓存
        redisTemplate.delete(RedisKeyPrefix.DEVICE_CACHE.generateFullKey(deviceId + ""));
    }

    @Override
    public PageInfo<Device> getDevicesByPage(int pageNum, int pageSize, Integer userId, String id, DeviceStatus status) {
        PageHelper.startPage(pageNum, pageSize);
        List<Device> devices = deviceMapper.selectByCondition(userId, id, status);
        return new PageInfo<>(devices);
    }

    @Override
    public List<Device> getDevices(Integer userId, String id, DeviceStatus status) {
        return deviceMapper.selectByCondition(userId, id, status);
    }

    /**
     * sse 发送设备更新事件
     *
     * @param userId 用户id
     */
    private void sendUpdateMessage(Integer userId) {
        // 提醒前端更新数据
        sseService.sendDataUpdateEvent(
                userId + "", "device-update"
        );
    }
}
