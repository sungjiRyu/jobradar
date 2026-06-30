package com.jobradar.backend.crawler.service.source;

import com.jobradar.backend.crawler.service.CrawledJobSaveService;
import com.jobradar.backend.global.time.BusinessTimeProvider;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class JobkoreaCrawlerServiceTest {

    private static final LocalDate FIXED_TODAY = LocalDate.of(2026, 6, 27);

    @Mock
    private CrawledJobSaveService crawledJobSaveService;

    private JobkoreaCrawlerService jobkoreaCrawlerService;

    @BeforeEach
    void setUp() {
        jobkoreaCrawlerService = new JobkoreaCrawlerService(
                crawledJobSaveService,
                new BusinessTimeProvider(java.time.Clock.fixed(
                        java.time.Instant.parse("2026-06-26T18:00:00Z"),
                        java.time.ZoneOffset.UTC
                ))
        );
    }

    @Test
    @DisplayName("게시일 파싱 - N시간 전 등록은 오늘 날짜")
    void parseListedAt_시간전() {
        Document doc = Jsoup.parse("""
                <table>
                    <tr class="devloopArea">
                        <td class="odd"><span class="time">3시간 전 등록</span></td>
                    </tr>
                </table>
                """);
        Element item = doc.selectFirst("tr.devloopArea");

        LocalDate result = jobkoreaCrawlerService.parseListedAt(item);

        assertThat(result).isEqualTo(FIXED_TODAY);
    }

    @Test
    @DisplayName("게시일 파싱 - N일 전 등록은 N일 전 날짜")
    void parseListedAt_일전() {
        Document doc = Jsoup.parse("""
                <table>
                    <tr class="devloopArea">
                        <td class="odd"><span class="time">2일 전 등록</span></td>
                    </tr>
                </table>
                """);
        Element item = doc.selectFirst("tr.devloopArea");

        LocalDate result = jobkoreaCrawlerService.parseListedAt(item);

        assertThat(result).isEqualTo(FIXED_TODAY.minusDays(2));
    }

    @Test
    @DisplayName("게시일 파싱 - 게시일 엘리먼트가 없으면 null")
    void parseListedAt_엘리먼트없음() {
        Document doc = Jsoup.parse("""
                <table>
                    <tr class="devloopArea"></tr>
                </table>
                """);
        Element item = doc.selectFirst("tr.devloopArea");

        LocalDate result = jobkoreaCrawlerService.parseListedAt(item);

        assertThat(result).isNull();
    }
}
