package com.jobradar.backend.crawler.service.source;

import com.jobradar.backend.crawler.service.CrawledJobSaveService;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class SaraminCrawlerServiceTest {

    @Mock
    private CrawledJobSaveService crawledJobSaveService;

    @InjectMocks
    private SaraminCrawlerService saraminCrawlerService;

    @Test
    @DisplayName("게시일 파싱 - 사람인 YY/MM/DD 형식")
    void parseListedAt_날짜형식() {
        Document doc = Jsoup.parse("""
                <div class="item_recruit">
                    <span class="job_day">수정일 26/05/11</span>
                </div>
                """);
        Element item = doc.selectFirst("div.item_recruit");

        LocalDate result = saraminCrawlerService.parseListedAt(item);

        assertThat(result).isEqualTo(LocalDate.of(2026, 5, 11));
    }

    @Test
    @DisplayName("게시일 파싱 - 게시일 엘리먼트가 없으면 null")
    void parseListedAt_엘리먼트없음() {
        Document doc = Jsoup.parse("<div class=\"item_recruit\"></div>");
        Element item = doc.selectFirst("div.item_recruit");

        LocalDate result = saraminCrawlerService.parseListedAt(item);

        assertThat(result).isNull();
    }
}
