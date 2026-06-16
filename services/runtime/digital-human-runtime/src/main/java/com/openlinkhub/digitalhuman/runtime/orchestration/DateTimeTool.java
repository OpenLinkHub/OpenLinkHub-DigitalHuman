package com.openlinkhub.digitalhuman.runtime.orchestration;

import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

@Component
public class DateTimeTool {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy年MM月dd日 EEEE HH:mm", Locale.CHINA);

    private final Clock clock;

    public DateTimeTool() {
        this(Clock.systemDefaultZone());
    }

    DateTimeTool(Clock clock) {
        this.clock = clock;
    }

    public String today() {
        return "今天是" + ZonedDateTime.now(clock).format(DATE_FORMATTER) + "。";
    }
}
