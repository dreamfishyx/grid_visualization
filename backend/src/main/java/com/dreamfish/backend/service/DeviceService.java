package com.dreamfish.backend.service;

import com.dreamfish.backend.entity.Device;
import com.dreamfish.backend.entity.status.DeviceStatus;
import com.dreamfish.backend.entity.User;
import com.github.pagehelper.PageInfo;

import java.util.List;

/**
 * @author Dream fish
 * @version 1.0
 * @description: 设备服务
 * @date 2025/4/3 21:18
 */
public interface DeviceService {

    /**
     * 根据设备id查询设备
     *
     * @param deviceId 设备id
     * @return 设备信息
     */
    Device findByDeviceId(Integer deviceId);

    /**
     * 查询数据库中的设备记录，无论设备是否处于删除状态
     *
     * @param deviceId 设备id
     * @return 设备信息
     */
    Device findDeviceRecord(Integer deviceId);

    /**
     * 注册设备
     *
     * @param device 设备信息
     */
    void register(Device device);

    /**
     * 重新注册设备
     *
     * @param device 设备信息
     */
    void reenrollDevice(Device device);

    /**
     * 更新设备状态(含乐观锁)
     *
     * @param device      设备信息
     * @param sendMessage 是否通知前端更新数据
     */
    void updateDeviceStatus(Device device, Boolean sendMessage);

    /**
     * 根据设备id判断设备是否存在
     *
     * @param deviceId 设备id
     * @return 是否存在
     */
    boolean existsByDeviceId(Integer deviceId);

    /**
     * 尝试维修设备
     *
     * @param deviceId 设备id
     */
    void doMaintenance(Integer deviceId);

    /**
     * 尝试维修设备
     *
     * @param user 用户信息(需要用户id)
     */
    void doMaintenance(User user);

    /**
     * 通过设备id获取所属用户id
     *
     * @param deviceId 设备id
     * @return 所属用户id
     */
    Integer getUserIdByDeviceId(Integer deviceId);

    /**
     * 删除设备,需要注意正在交给维修的设备不允许删除。
     *
     * @param deviceId 设备id
     */
    void deleteDevice(Integer deviceId);

    /**
     * 获取分页数据列表
     *
     * @param pageNum  页码
     * @param pageSize 页面数据大小
     * @param userId   用户id
     * @param id       设备id过滤条件
     * @param status   状态过滤条件
     * @return 分页数据列表
     */
    PageInfo<Device> getDevicesByPage(int pageNum, int pageSize, Integer userId, String id, DeviceStatus status);

    /**
     * 获取所有设备列表
     *
     * @param userId 用户id
     * @param id     设备id过滤条件
     * @param status 状态过滤条件
     * @return 设备列表
     */
    List<Device> getDevices(Integer userId, String id, DeviceStatus status);
}
