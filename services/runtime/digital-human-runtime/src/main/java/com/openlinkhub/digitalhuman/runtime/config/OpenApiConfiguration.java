package com.openlinkhub.digitalhuman.runtime.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfiguration {

    @Bean
    public OpenAPI digitalHumanRuntimeOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("OpenLinkHub Digital Human Runtime API")
                        .version("v1.0.0")
                        .description("Realtime digital human runtime APIs for ASR, RAG, TTS, and conversation orchestration.")
                        .contact(new Contact()
                                .name("OpenLinkHub DigitalHuman")
                                .url("https://github.com/OpenLinkHub")));
    }
}
