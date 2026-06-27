package com.jobradar.backend.global.time;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;

class BusinessTimeProviderTest {

    @Test
    @DisplayName("UTC 기준 날짜와 달라도 서비스 오늘은 Asia/Seoul 기준으로 계산한다")
    void today_usesAsiaSeoulBusinessZone() {
        BusinessTimeProvider provider = new BusinessTimeProvider(
                Clock.fixed(Instant.parse("2026-06-26T18:00:00Z"), ZoneOffset.UTC)
        );

        assertThat(provider.today()).isEqualTo(LocalDate.of(2026, 6, 27));
    }
}
