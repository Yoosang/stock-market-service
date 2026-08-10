package com.usang.stockmarket.global.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI stockMarketOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("Stock Market Service API")
                        .description("인증, 관심종목, 실시간 시세, 뉴스, 가격 알림을 제공하는 백엔드 API")
                        .version("v0.0.1"));
    }
}
