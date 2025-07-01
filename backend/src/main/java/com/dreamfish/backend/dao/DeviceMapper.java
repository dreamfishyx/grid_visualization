package com.dreamfish.backend.dao;

import com.dreamfish.backend.entity.Device;
import com.dreamfish.backend.entity.status.DeviceStatus;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * @author Dream fish
 * @version 1.0
 * @description: TODO
 * @date 2025/4/5 16:48
 */
@Mapper
public interface DeviceMapper {
    /**
     * 通过设备id查询设备
     *
     * @param deviceId 设备id
     * @return 设备信息
     */
    Device findByDeviceId(
            @Param("deviceId") int deviceId
    );

    /**
     * 通过设备id查询记录,不论是否逻辑删除
     *
     * @param deviceId 设备id
     * @return 设备信息
     */
    Device findDeviceRecord(
            @Param("deviceId") int deviceId
    );

    /**
     * 新增设备
     *
     * @param device 设备信息
     * @return 设备主键
     */
    int insertDevice(Device device);

    /**
     * 更新设备状态
     *
     * @param device 设备信息
     * @return 影响行数
     */
    int updateDeviceStatus(Device device);

    /**
     * 重新注册设备
     *
     * @param device 设备信息
     * @return 影响行数
     */
    int reenrollDevice(Device device);

    /**
     * 删除设备
     *
     * @param device 设备信息
     * @return 影响行数
     */
    Integer deleteDevice(Device device);


    /**
     * 按照条件查询设备
     *
     * @param userId          用户id
     * @param deviceIdPattern 设备id匹配条件
     * @param status          状态
     * @return 设备列表
     */
    List<Device> selectByCondition(
            @Param("userId") Integer userId,
            @Param("deviceId") String deviceIdPattern,
            @Param("status") DeviceStatus status
    );
}
