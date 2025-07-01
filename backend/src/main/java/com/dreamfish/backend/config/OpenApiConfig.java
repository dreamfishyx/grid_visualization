package com.dreamfish.backend.config;

import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author Dream fish
 * @version 1.0
 * @description: OpenAPI配置
 * @date 2025/4/28 10:19
 */
@Configuration
@SecurityScheme(
        name = "BearerAuth",
        type = SecuritySchemeType.HTTP,
        scheme = "bearer",
        bearerFormat = "JWT"
)
public class OpenApiConfig {
    @Bean
    public OpenAPI customOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("API文档")
                        .version("6.0.0")
                        .description("智能接地线在线监控系统")
                        .contact(new Contact()
                                .name("@Dream fish")
                                .email("dreamfishyx@qq.com")
                        )
                );
    }
}
