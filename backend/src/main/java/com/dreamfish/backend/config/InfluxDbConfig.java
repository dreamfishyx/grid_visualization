package com.dreamfish.backend.config;

import com.influxdb.client.BucketsApi;
import com.influxdb.client.InfluxDBClient;
import com.influxdb.client.InfluxDBClientFactory;
import com.influxdb.client.OrganizationsApi;
import com.influxdb.client.domain.Bucket;
import com.influxdb.client.domain.BucketRetentionRules;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Collections;

/**
 * @author Dream fish
 * @version 1.0
 * @description: InfluxDB配置类
 * @date 2025/4/1 18:05
 */
@Configuration
@Slf4j
public class InfluxDbConfig {

    @Value("${influx.url}")
    private String influxUrl;

    @Value("${influx.token}")
    private String influxToken;

    @Value("${influx.org}")
    private String influxOrg;

    @Value("${influx.bucket}")
    private String influxBucket;

    @Value("${influx.retention-days:7}")
    private int retentionDays;

    @Bean
    public InfluxDBClient influxDbClient() {
        InfluxDBClient client = InfluxDBClientFactory.create(
                influxUrl,
                influxToken.toCharArray(),
                influxOrg,
                influxBucket
        );

        setupRetentionPolicy(client);

        return client;
    }

    /**
     * 设置 Retention Policy
     *
     * @param client InfluxDBClient
     */
    public void setupRetentionPolicy(InfluxDBClient client) {
        try {
            BucketsApi bucketsApi = client.getBucketsApi();
            Bucket existingBucket = bucketsApi.findBucketByName(influxBucket);

            BucketRetentionRules retentionRule = new BucketRetentionRules();

            retentionRule.setEverySeconds(60 * 60 * 24 * retentionDays);

            String orgId = getOrgId(client);

            if (existingBucket == null) {
                Bucket newBucket = new Bucket()
                        .name(influxBucket)
                        .retentionRules(Collections.singletonList(retentionRule))
                        .orgID(orgId);
                bucketsApi.createBucket(newBucket);
                log.info("Created InfluxDB bucket: {}", influxBucket);
            } else {
                existingBucket.setRetentionRules(Collections.singletonList(retentionRule));
                bucketsApi.updateBucket(existingBucket);
                log.info("Updated retention policy for bucket: {}", influxBucket);
            }
        } catch (Exception e) {
            log.error("Failed to setup InfluxDB bucket: {}", e.getMessage());
            throw new RuntimeException("InfluxDB initialization failed", e);
        }
    }

    /**
     * 根据组织名称获取 OrgId
     *
     * @param client InfluxDBClient
     * @return OrgId
     */
    private String getOrgId(InfluxDBClient client) {
        OrganizationsApi orgApi = client.getOrganizationsApi();
        return orgApi.findOrganizations()
                .stream()
                .filter(org -> org.getName().equals(influxOrg))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Organization '" + influxOrg + "' not found"))
                .getId();
    }
}