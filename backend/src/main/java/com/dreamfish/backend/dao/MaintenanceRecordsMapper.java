package com.dreamfish.backend.dao;

import com.dreamfish.backend.entity.MaintenanceRecord;
import com.dreamfish.backend.entity.status.MaintenanceRecordStatus;
import org.apache.ibatis.annotations.Mapper;
import org.springframework.data.repository.query.Param;

import java.sql.Date;
import java.util.List;

/**
 * @author Dream fish
 * @version 1.0
 * @description: 维修记录 mapper 接口
 * @date 2025/4/12 17:37
 */
@Mapper
public interface MaintenanceRecordsMapper {
    /**
     * 新增维修记录
     *
     * @param maintenanceRecord 维修记录
     */
    int insertMaintenanceRecords(MaintenanceRecord maintenanceRecord);

    /**
     * 更新维修记录数据
     *
     * @param maintenanceRecord 维修记录数据
     */
    int updateMaintenanceRecords(MaintenanceRecord maintenanceRecord);

    /**
     * 按照条件查询维修记录
     *
     * @param userId    用户id
     * @param search    搜索条件
     * @param status    状态
     * @param startTime 开始时间
     * @param endTime   结束时间
     * @return 维修记录列表
     */
    List<MaintenanceRecord> findAllMaintenanceRecords(
            @Param("userId") Integer userId,
            @Param("search") String search,
            @Param("status") MaintenanceRecordStatus status,
            @Param("startTime") Date startTime,
            @Param("endTime") Date endTime
    );

    /**
     * 通过id查找维修记录
     *
     * @param id id
     * @return 维系记录
     */
    MaintenanceRecord findMaintenanceRecordById(@Param("id") Integer id);

    /**
     * 通过设备id查找维修记录
     *
     * @param deviceId 设备id
     * @return 围嘴记录
     */
    List<MaintenanceRecord> findMaintenanceRecordByDeviceId(@Param("deviceId") Integer deviceId);

    /**
     * 通过设备id删除维修记录
     *
     * @param deviceId 设备id
     * @return 受影响行数
     */
    int deleteRecordWithDeviceId(Integer deviceId);
}
