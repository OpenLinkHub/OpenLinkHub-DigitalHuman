package com.openlinkhub.digitalhuman.runtime.orchestration;

import org.springframework.stereotype.Component;

@Component
public class WeatherTool {

    private final String defaultLocation;

    public WeatherTool() {
        this("江门");
    }

    WeatherTool(String defaultLocation) {
        this.defaultLocation = defaultLocation;
    }

    public String today() {
        return unavailable("今天", defaultLocation);
    }

    public String weekly() {
        return unavailable("未来一周", defaultLocation);
    }

    private String unavailable(String range, String location) {
        return "天气服务尚未配置，暂时无法查询" + location + range + "天气。";
    }
}
