package com.dreamfish.backend.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * @author Dream fish
 * @version 1.0
 * @description: 地图配置类
 * @date 2025/4/12 14:02
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "map")
public class MapConfigProperties {
    private String key;
    private String geocodeUrl;
    private String markerUrl;
    private String src;
    private String coordinate;
}