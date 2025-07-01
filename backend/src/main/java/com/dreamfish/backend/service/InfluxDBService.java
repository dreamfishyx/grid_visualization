package com.dreamfish.backend.service;

import com.dreamfish.backend.entity.Device;

import java.math.BigDecimal;
import java.util.List;

/**
 * @author Dream fish
 * @version 1.0
 * @description: TODO
 * @date 2025/4/5 16:42
 */

public interface InfluxDBService {

     void save(Device device);

     List<BigDecimal> analyseHourAgo(int deviceId) ;

     List<BigDecimal> analyseLast12Hours(int deviceId);

}
