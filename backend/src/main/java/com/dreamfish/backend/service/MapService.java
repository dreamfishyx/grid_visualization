package com.dreamfish.backend.service;

import com.dreamfish.backend.common.ErrorCode;
import com.dreamfish.backend.config.MapConfigProperties;
import com.dreamfish.backend.exception.MapServiceException;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;

/**
 * @author Dream fish
 * @version 1.0
 * @description: TODO
 * @date 2025/4/12 14:03
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class MapService {
    private final MapConfigProperties mapConfig;
    private final RestTemplate restTemplate;


    /**
     * 坐标转地址
     *
     * @param longitude 经度
     * @param latitude  维度
     * @return 地址信息
     */
    @Cacheable(value = "geocodeCache",
            key = "T(java.util.Arrays).toString(new Double[]{#longitude, #latitude})")
    public AddressInfo getAddressByCoordinate(double longitude, double latitude) {
        String url = String.format("%s?key=%s&location=%f,%f&radius=1000&extensions=base",
                mapConfig.getGeocodeUrl(),
                mapConfig.getKey(),
                longitude,
                latitude);

        try {
            log.info("高德api获取url:{}", url);
            ResponseEntity<GeoResponse> response = restTemplate.getForEntity(url, GeoResponse.class);
            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                log.info("地图服务响应: {}", response.getBody());
                return processGeoResponse(response.getBody());
            }
            throw new MapServiceException(ErrorCode.MAP_SERVICE_ERROR, "地图服务响应异常");
        } catch (RestClientException e) {
            log.error("地图服务调用失败", e);
            throw new MapServiceException(ErrorCode.MAP_SERVICE_ERROR, "地图服务调用失败");
        }
    }

    //

    /**
     * 生成导航标记跳转链接
     *
     * @param longitude 经度
     * @param latitude  纬度
     * @param name      标记名称
     * @return 导航链接
     */
    public String generateMarkerUrl(double longitude, double latitude, String name) {
        String s = String.format("%s?position=%f,%f&name=%s&src=%s&coordinate=%s&callnative=1",
                mapConfig.getMarkerUrl(),
                longitude, latitude,
                URLEncoder.encode(name, StandardCharsets.UTF_8),
                mapConfig.getSrc(),
                mapConfig.getCoordinate());
        log.info("生成导航链接: {}", s);

        return s;
    }

    private AddressInfo processGeoResponse(GeoResponse response) {
        if ("1".equals(response.getStatus())) {
            GeoResponse.Regeocode regeocode = response.getRegeocode();
            GeoResponse.AddressComponent component = regeocode.getAddressComponent();

            // 合并 formatted_address 数组为字符串
            //String fullAddress = String.join(", ", regeocode.getFormatted_address());

            // 提取 city 列表的第一个值
            String city = (component.getCity() != null && !component.getCity().isEmpty())
                    ? component.getCity().getFirst()
                    : "";
            GeoResponse.AddressComponent addressComponent = new GeoResponse.AddressComponent();
            addressComponent.setProvince(component.getProvince());
            addressComponent.setCity(Collections.singletonList(city));
            addressComponent.setDistrict(component.getDistrict());
            addressComponent.setTownship(component.getTownship());

            return new AddressInfo(regeocode.getFormatted_address(), addressComponent);
            //return new AddressInfo(fullAddress, addressComponent);
        }
        throw new MapServiceException(ErrorCode.MAP_SERVICE_ERROR, "地图服务响应异常");
    }

    // 响应数据结构
    @Data
    public static class GeoResponse {
        // 1 表示成功
        private String status;
        // 说明
        private String info;
        // 地理编码
        private Regeocode regeocode;

        @Data
        public static class Regeocode {
            private String formatted_address;
            private AddressComponent addressComponent;
        }

        @Data
        public static class AddressComponent {
            private String province;
            private List<String> city;
            private String district;
            private String township;
        }
    }

    // 地址信息DTO
    public record AddressInfo(String fullAddress, GeoResponse.AddressComponent addressComponent) {
    }

}