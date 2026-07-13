package com.hotel.common.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SpringDocConfig {

    @Bean
    public OpenAPI hotelOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("酒店辅助订购系统 API")
                        .description("Hotel Assisted Ordering System — RESTful API 文档")
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("Hotel Dev Team")));
    }
}
