package com.openlinkhub.digitalhuman.funasr;

import com.openlinkhub.digitalhuman.funasr.config.FunAsrProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties(FunAsrProperties.class)
public class DashScopeFunAsrRealtimeApplication {

    public static void main(String[] args) {
        SpringApplication.run(DashScopeFunAsrRealtimeApplication.class, args);
    }
}
