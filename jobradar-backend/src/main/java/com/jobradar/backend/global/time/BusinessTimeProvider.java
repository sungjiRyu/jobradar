package com.jobradar.backend.global.time;

import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;

/**
 * JobRadar 서비스의 비즈니스 기준 시간을 제공한다.
 *
 * "오늘", "내일", "마감일" 같은 날짜 판단은 서버/JVM 기본 timezone이 아니라
 * 한국 채용 서비스 기준 timezone(Asia/Seoul)을 명시적으로 사용한다.
 */
@Component
public class BusinessTimeProvider {

    public static final ZoneId BUSINESS_ZONE = ZoneId.of("Asia/Seoul");

    private final Clock clock;

    public BusinessTimeProvider() {
        this(Clock.system(BUSINESS_ZONE));
    }

    public BusinessTimeProvider(Clock clock) {
        this.clock = clock.withZone(BUSINESS_ZONE);
    }

    public LocalDate today() {
        return LocalDate.now(clock);
    }

    public LocalDateTime now() {
        return LocalDateTime.now(clock);
    }

    public LocalDateTime startOfToday() {
        return today().atStartOfDay();
    }

    public LocalDateTime startOfTomorrow() {
        return today().plusDays(1).atStartOfDay();
    }
}
