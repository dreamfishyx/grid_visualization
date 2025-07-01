package com.dreamfish.backend.config;

import com.dreamfish.backend.common.ErrorCode;
import com.dreamfish.backend.exception.MapServiceException;
import org.jetbrains.annotations.NotNull;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.client.BufferingClientHttpRequestFactory;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.ResponseErrorHandler;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.net.URI;
import java.time.Duration;

/**
 * @author Dream fish
 * @version 1.0
 * @description: RestTemplate配置
 * @date 2025/4/12 14:27
 */
@Configuration
public class RestTemplateConfig {

    @Bean
    public RestTemplate restTemplate(RestTemplateBuilder builder) {
        return builder
                .connectTimeout(Duration.ofSeconds(5))
                .readTimeout(Duration.ofSeconds(8))
                .requestFactory(() -> new BufferingClientHttpRequestFactory(new SimpleClientHttpRequestFactory()))
                .errorHandler(new RestTemplateErrorHandler())
                .build();
    }

    // 自定义错误处理器
    private static class RestTemplateErrorHandler implements ResponseErrorHandler {
        @Override
        public boolean hasError(ClientHttpResponse response) throws IOException {
            return response.getStatusCode().isError();
        }

        @Override
        public void handleError(@NotNull URI url, @NotNull HttpMethod method, ClientHttpResponse response) throws IOException {
            throw new MapServiceException(ErrorCode.MAP_SERVICE_ERROR, "地图服务调用失败,状态码:" + response.getRawStatusCode());
        }
    }
}
