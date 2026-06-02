package com.jobradar.backend.global.config;

import jakarta.annotation.PostConstruct;
import org.springframework.context.annotation.Configuration;

import java.util.TimeZone;

@Configuration
public class TimeZoneConfig {

    private static final String APPLICATION_TIME_ZONE = "Asia/Seoul";

    @PostConstruct
    public void setDefaultTimeZone() {
        TimeZone.setDefault(TimeZone.getTimeZone(APPLICATION_TIME_ZONE));
    }
}
