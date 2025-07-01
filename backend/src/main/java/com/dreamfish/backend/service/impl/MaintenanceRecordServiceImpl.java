package com.dreamfish.backend.service.impl;

import com.dreamfish.backend.common.ErrorCode;
import com.dreamfish.backend.dao.MaintenanceRecordsMapper;
import com.dreamfish.backend.entity.MaintenanceRecord;
import com.dreamfish.backend.entity.status.MaintenanceRecordStatus;
import com.dreamfish.backend.exception.MaintenanceRecordsException;
import com.dreamfish.backend.service.MaintenanceRecordService;
import com.dreamfish.backend.service.SSEService;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.sql.Date;
import java.util.List;

/**
 * @author Dream fish
 * @version 1.0
 * @description: 维修记录 service 实现类
 * @date 2025/4/12 17:46
 */
@Service
@RequiredArgsConstructor
public class MaintenanceRecordServiceImpl implements MaintenanceRecordService {
    private final MaintenanceRecordsMapper recordsMapper;
    private final SSEService sseService;

    @Override
    public void insertMaintenanceRecords(MaintenanceRecord maintenanceRecord) {
        recordsMapper.insertMaintenanceRecords(maintenanceRecord);

        sendUpdateMessage(maintenanceRecord.getUserId());
    }

    @Override
    public void updateMaintenanceRecords(MaintenanceRecord maintenanceRecord) {
        int i = recordsMapper.updateMaintenanceRecords(maintenanceRecord);
        if (i == 0) {
            throw new MaintenanceRecordsException(
                    ErrorCode.DB_OPERATION_FAILED, "更新失败"
            );
        }

        sendUpdateMessage(maintenanceRecord.getUserId());
    }

    @Override
    public PageInfo<MaintenanceRecord> findAllMaintenanceRecords(
            Integer pageNum,
            Integer pageSize,
            Integer userId,
            String search,
            MaintenanceRecordStatus status,
            Date startTime,
            Date endTime
    ) {
        PageHelper.startPage(pageNum, pageSize);
        List<MaintenanceRecord> records = recordsMapper.findAllMaintenanceRecords(
                userId,
                search,
                status,
                startTime,
                endTime);
        return new PageInfo<>(records);
    }

    @Override
    public MaintenanceRecord findMaintenanceRecordById(Integer id) {
        MaintenanceRecord record = recordsMapper.findMaintenanceRecordById(id);
        if (record == null) {
            throw new RuntimeException("维修记录不存在");
        }
        return record;
    }

    @Override
    public Boolean isDeviceOccupancy(Integer deviceId) {
        List<MaintenanceRecord> records = recordsMapper.findMaintenanceRecordByDeviceId(deviceId);
        if (records == null || records.isEmpty()) {
            return false;
        }
        // 遍历维修记录
        for (MaintenanceRecord record : records) {
            if (record.getStatus() == MaintenanceRecordStatus.UNFINISHED) {
                return true;
            }
        }
        return false;
    }

    @Override
    public int deleteRecordWithDeviceId(Integer deviceId) {
        return recordsMapper.deleteRecordWithDeviceId(deviceId);
    }

    private void sendUpdateMessage(Integer userId) {
        // 提醒前端更新数据
        sseService.sendDataUpdateEvent(
                userId + "", "maintenance-update"
        );
    }


}
