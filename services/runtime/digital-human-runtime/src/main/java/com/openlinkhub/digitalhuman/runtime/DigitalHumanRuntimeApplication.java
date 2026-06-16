package com.openlinkhub.digitalhuman.runtime;

import com.openlinkhub.digitalhuman.runtime.config.FunAsrProperties;
import com.openlinkhub.digitalhuman.runtime.config.IntentRoutingProperties;
import com.openlinkhub.digitalhuman.runtime.config.LightRagProperties;
import com.openlinkhub.digitalhuman.runtime.config.TtsProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties({FunAsrProperties.class, LightRagProperties.class, TtsProperties.class, IntentRoutingProperties.class})
public class DigitalHumanRuntimeApplication {

    public static void main(String[] args) {
        SpringApplication.run(DigitalHumanRuntimeApplication.class, args);
    }
}
