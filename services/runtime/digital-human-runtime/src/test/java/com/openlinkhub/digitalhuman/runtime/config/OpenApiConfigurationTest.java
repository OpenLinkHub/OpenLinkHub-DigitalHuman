package com.openlinkhub.digitalhuman.runtime.config;

import static org.assertj.core.api.Assertions.assertThat;

import io.swagger.v3.oas.models.OpenAPI;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

class OpenApiConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(OpenApiConfiguration.class);

    @Test
    void exposesOpenApiMetadataForKnife4j() {
        contextRunner.run(context -> {
            assertThat(context).hasSingleBean(OpenAPI.class);

            OpenAPI openAPI = context.getBean(OpenAPI.class);
            assertThat(openAPI.getInfo().getTitle()).isEqualTo("OpenLinkHub Digital Human Runtime API");
            assertThat(openAPI.getInfo().getVersion()).isEqualTo("v1.0.0");
        });
    }
}
