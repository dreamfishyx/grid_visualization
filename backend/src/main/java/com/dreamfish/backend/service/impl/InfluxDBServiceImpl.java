package com.dreamfish.backend.service.impl;

import com.dreamfish.backend.common.ErrorCode;
import com.dreamfish.backend.entity.Device;
import com.dreamfish.backend.exception.BusinessException;
import com.dreamfish.backend.service.InfluxDBService;
import com.dreamfish.backend.service.SSEService;
import com.influxdb.client.InfluxDBClient;
import com.influxdb.client.domain.WritePrecision;
import com.influxdb.client.write.Point;
import com.influxdb.query.FluxRecord;
import com.influxdb.query.FluxTable;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Dream fish
 * @version 1.0
 * @description: influxDB service
 * @date 2025/4/5 16:42
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class InfluxDBServiceImpl implements InfluxDBService {
    private final InfluxDBClient influxClient;
    private final SSEService sseService;

    @Value("${influx.bucket}")
    private String bucket;
    @Value("${influx.org}")
    private String myOrg;

    @Override
    public void save(Device device) {
        log.info("保存数据到influxDB: {}", device);
        Point point = Point.measurement("sensor_data")
                .addTag("deviceId", String.valueOf(device.getDeviceId()))
                .addField("resistance", device.getResistance().doubleValue())
                .time(device.getReceiveTime(), WritePrecision.NS);
        influxClient.getWriteApiBlocking().writePoint(bucket, myOrg, point);

        // 发送sse消息
        sendUpdateMessage(device.getDeviceId());
    }

    // 查询方法实现:按照id,查询过去一小时的数据中每五分钟的平均值
    @Override
    public List<BigDecimal> analyseHourAgo(int deviceId) {
        // 构造Flux查询语句
        String fluxQuery = String.format(
                """
                        from(bucket: "%s")
                          |> range(start: -1h)
                          |> filter(fn: (r) => r._measurement == "sensor_data" and r.deviceId == "%s")
                          |> filter(fn: (r) => r._field == "resistance")
                          |> aggregateWindow(every: 5m, fn: mean, createEmpty: true)
                          |> fill(value: 0.0)
                          |> sort(columns: ["_time"])""",
                bucket, deviceId
        );

        return queryResistance(fluxQuery);
    }

    // 查询过去12小时的数据，按照小时聚合求平均值
    @Override
    public List<BigDecimal> analyseLast12Hours(int deviceId) {
        String fluxQuery = String.format(
                """
                        from(bucket: "%s")
                          |> range(start: -12h)
                          |> filter(fn: (r) => r._measurement == "sensor_data" and r.deviceId == "%s")
                          |> filter(fn: (r) => r._field == "resistance")
                          |> aggregateWindow(every: 1h, fn: mean, createEmpty: true)
                          |> fill(value: 0.0)
                          |> sort(columns: ["_time"])""",
                bucket, deviceId
        );
        return queryResistance(fluxQuery);
    }

    /**
     * 查询数据
     *
     * @param fluxQuery 查询语句
     * @return 查询结果(若是空 ， 返回空列表)
     */
    public List<BigDecimal> queryResistance(String fluxQuery) {
        List<BigDecimal> results = new ArrayList<>();
        try {
            // 执行查询
            List<FluxTable> tables = influxClient.getQueryApi().query(fluxQuery, this.myOrg);

            // 解析结果
            for (FluxTable table : tables) {
                for (FluxRecord record : table.getRecords()) {
                    Object value = record.getValue();
                    if (value instanceof Number) {
                        results.add(new BigDecimal(value.toString()));
                    }
                }
            }
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.DB_SELECT_CODE, "influxDB查询失败");
        }
        return results;
    }

    private void sendUpdateMessage(Integer deviceId) {
        // 提醒前端更新数据
        sseService.sendDataUpdateEvent(
                deviceId + "", "device-data-update:"+deviceId
        );
    }

}
