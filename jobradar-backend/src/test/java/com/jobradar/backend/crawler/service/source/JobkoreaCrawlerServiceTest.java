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
class JobkoreaCrawlerServiceTest {

    @Mock
    private CrawledJobSaveService crawledJobSaveService;

    @InjectMocks
    private JobkoreaCrawlerService jobkoreaCrawlerService;

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

        assertThat(result).isEqualTo(LocalDate.now());
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

        assertThat(result).isEqualTo(LocalDate.now().minusDays(2));
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
