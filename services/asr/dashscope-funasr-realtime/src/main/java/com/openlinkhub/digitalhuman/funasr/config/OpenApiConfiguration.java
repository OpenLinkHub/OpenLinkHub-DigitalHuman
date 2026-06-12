package com.openlinkhub.digitalhuman.funasr.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfiguration {

    @Bean
    public OpenAPI dashScopeFunAsrOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("DashScope FunASR Realtime API")
                        .version("v1.0.0")
                        .description("Spring Boot service for Alibaba Cloud DashScope FunASR realtime speech recognition.")
                        .contact(new Contact()
                                .name("OpenLinkHub DigitalHuman")
                                .url("https://github.com/OpenLinkHub")));
    }
}
