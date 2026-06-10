package com.example.home.global.config;


import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@OpenAPIDefinition(info = @Info(title="EstateFlow API", description="부동산 정책 충격 전파 분석 시스템 API", version="v1"))
public class SwaggerConfig {

    @Bean
    GroupedOpenApi memberOpenAPI() {
        String[] paths = {"/api/members/**"};
        return GroupedOpenApi.builder()
            .group("Member 관련 API")
            .pathsToMatch(paths)
            .build();
    }

}
