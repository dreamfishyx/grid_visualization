package com.dreamfish.backend.service;

import com.dreamfish.backend.entity.MaintenanceRecord;
import com.dreamfish.backend.entity.status.MaintenanceRecordStatus;
import com.github.pagehelper.PageInfo;

import java.sql.Date;

/**
 * @author Dream fish
 * @version 1.0
 * @description: 维修记录服务
 * @date 2025/4/12 17:45
 */
public interface MaintenanceRecordService {

    /**
     * 新增维修记录
     *
     * @param maintenanceRecord 维修记录数据
     */
    void insertMaintenanceRecords(MaintenanceRecord maintenanceRecord);

    /**
     * 更新维修记录数据
     *
     * @param maintenanceRecord 维修记录数据
     */
    void updateMaintenanceRecords(MaintenanceRecord maintenanceRecord);


    /**
     * 维修记录分页展示
     *
     * @param pageNum   页码
     * @param pageSize  页面数据大小
     * @param userId    用户id
     * @param search    过滤条件
     * @param status    状态
     * @param startTime 开始时间
     * @param endTime   结束时间
     * @return 分页信息
     */
    PageInfo<MaintenanceRecord> findAllMaintenanceRecords(
            Integer pageNum,
            Integer pageSize,
            Integer userId,
            String search,
            MaintenanceRecordStatus status,
            Date startTime,
            Date endTime
    );

    /**
     * 通过维修记录id查询维修记录
     *
     * @param id id
     * @return 维修记录
     */
    MaintenanceRecord findMaintenanceRecordById(Integer id);

    /**
     * 判断设备是否被占用
     *
     * @param deviceId 设备id
     * @return 设备占用情况
     */
    Boolean isDeviceOccupancy(Integer deviceId);

    /**
     * 通过设备id删除维修记录
     *
     * @param deviceId 设备id
     * @return 受影响行数
     */
    int deleteRecordWithDeviceId(Integer deviceId);

}
