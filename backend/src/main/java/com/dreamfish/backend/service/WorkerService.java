package com.dreamfish.backend.service;

import com.dreamfish.backend.entity.Device;
import com.dreamfish.backend.entity.Worker;
import com.github.pagehelper.PageInfo;

/**
 * @author Dream fish
 * @version 1.0
 * @description: TODO
 * @date 2025/4/11 15:37
 */
public interface WorkerService {
    /**
     * 注册维修人员到当前用户
     *
     * @param worker 维修人员
     */
    void register(Worker worker);

    /**
     * 通过维修人员id查询
     *
     * @param workerId 维修人员id
     * @return 维修人员信息
     */
    Worker findById(Integer workerId);

    /**
     * 更新维修人员
     *
     * @param worker 维修人员信息
     */
    void updateWorker(Worker worker);

    /**
     * 删除维修人员
     *
     * @param workerId 维修人员id
     */
    void deleteWorker(Integer workerId);

    /**
     * 获取维修人员分页列表
     *
     * @param pageNum  页码
     * @param pageSize 页面数据大小
     * @param worker   维修人员查询条件信息
     * @return 分页数据 ,
     */
    PageInfo<Worker> findAllWorkers(Integer pageNum, Integer pageSize, Worker worker);

    /**
     * 分配一个空闲员工
     *
     * @return 员工信息
     */
    Worker assignWorker();

    /**
     * 释放一个员工
     *
     * @param userId 员工id
     */
    void releaseWorker(Integer userId);

    /**
     * 为维修人员分配一个设备维修任务
     *
     * @param worker 维修人员信息
     * @param device 待维修设备信息
     */
    void assignTask(Worker worker, Device device);

    /**
     * 根据设备id获取设备所属的用户id
     *
     * @param deviceId 设备id
     * @return 所属用户 id
     */
    Integer getUserIdByDeviceId(Integer deviceId);

    /**
     * 更新维修人员状态
     *
     * @param worker 维修人员数据
     */
    void updateWorkerStatus(Worker worker);
}
