package com.dreamfish.backend.config;

import com.dreamfish.backend.parser.BinaryDataArgumentResolver;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;

/**
 * @author Dream fish
 * @version 1.0
 * @description: 配置跨域请求、拦截器、参数解析器
 * @date 2025/4/1 20:05
 */
@Configuration
public class MvcConfig implements WebMvcConfigurer {

//    @Override
//    public void addCorsMappings(CorsRegistry registry) {
//
//        // 前端跨域请求
//        registry.addMapping("/api/backend/**")
//                .allowedOrigins("127.0.0.1")
//                // 允许所有请求方法跨域调用,也可设置为*
//                .allowedMethods("POST", "GET", "PUT", "OPTIONS", "DELETE")
//                .allowedHeaders("*")
//                .allowCredentials(true)
//                .maxAge(168000);
//
//        // 允许所有物联网设备跨域调用
//        registry.addMapping("/api/device/**")
//                .allowedOrigins("*")
//                .allowedMethods("POST", "PUT")
//                .allowedHeaders("*")
//                .allowCredentials(true)
//                .maxAge(168000);
//    }

//    @Override
//    public void addInterceptors(@NotNull InterceptorRegistry registry) {
//        WebMvcConfigurer.super.addInterceptors(registry);
//        LoginInterceptor loginInterceptor = new LoginInterceptor();
//        registry.addInterceptor(loginInterceptor)
//                .addPathPatterns("/**")
//                .excludePathPatterns(
//                        "/login", "/register", "/logout");
//    }

    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
        resolvers.add(new BinaryDataArgumentResolver());
    }
}