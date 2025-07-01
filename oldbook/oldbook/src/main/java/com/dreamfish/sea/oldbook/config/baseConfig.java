package com.dreamfish.sea.oldbook.config;

import com.alibaba.druid.pool.DruidDataSource;
import com.dreamfish.sea.oldbook.component.LoginInterceptor;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import javax.sql.DataSource;
import java.sql.SQLException;

/**
 * @Author: Dream fish
 * @Date: 2023/9/26 15:59
 * @Description: TODO
 * @Version: 1.0
 **/
@SpringBootConfiguration
public class baseConfig implements WebMvcConfigurer {

    @Bean
    @ConfigurationProperties(prefix = "spring.datasource")  //配置数据源前缀
    public DataSource dataSource() throws SQLException {
        DruidDataSource druidDataSource = new DruidDataSource();
        return druidDataSource;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        WebMvcConfigurer.super.addInterceptors(registry);
        LoginInterceptor loginInterceptor = new LoginInterceptor();
        registry.addInterceptor(loginInterceptor)
                .addPathPatterns("/**")
                .excludePathPatterns("/", "/to/login", "/to/register",
                        "/user/login", "/user/register",
                        "/css/**", "/js/**", "/favicon.ico",
                        "/pic/**", "/fonts/**");
    }

    @Override // 添加视图控制器
    public void addViewControllers(ViewControllerRegistry registry) {
        WebMvcConfigurer.super.addViewControllers(registry);

        //===浏览器发送/或/to/login请求来到login.html===
        registry.addViewController("/").setViewName("login");
        registry.addViewController("/to/login").setViewName("login");

        //===浏览器发送/to/register请求来到register.html===
        registry.addViewController("/to/register").setViewName("register");

        //===浏览器发送/to/create请求来到create.html===
        //registry.addViewController("/to/create").setViewName("create");
    }


}
