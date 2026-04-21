package com.jobradar.backend.global.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

/**
 * Google Gemini API를 사용한 채용공고 AI 요약 서비스
 *
 * [왜 Gemini를 쓰나?]
 * - 무료 티어 제공 (gemini-2.0-flash 기준 1,000건/일)
 * - 한국어 지원 우수
 * - Spring RestClient로 별도 SDK 없이 HTTP 호출 가능
 *
 * [면접 포인트]
 * - RestClient: Spring 6.1(Boot 3.2+)에서 추가된 동기 HTTP 클라이언트
 *   WebClient보다 단순하고, RestTemplate보다 최신 방식
 * - API Key 환경변수 주입: 코드에 키를 직접 넣지 않아 보안 유지
 */
@Slf4j
@Service
public class AiSummaryService {

    // Gemini API 엔드포인트 (모델명은 application.yml에서 주입)
    private static final String GEMINI_URL =
            "https://generativelanguage.googleapis.com/v1beta/models/{model}:generateContent?key={key}";

    @Value("${gemini.api-key:}")
    private String apiKey;

    @Value("${gemini.model:}")
    private String model;

    // RestClient는 매 요청마다 create()로 생성해도 가볍지만, 재사용을 위해 빌더로 관리
    private final RestClient restClient = RestClient.create();

    /**
     * 채용공고 description을 받아 200자 이내 한국어 요약 반환
     *
     * @param description 공고 전체 텍스트
     * @return 요약 문자열 (API 키 없거나 실패 시 null)
     */
    public String summarize(String description) {
        // API 키가 설정되지 않으면 조용히 스킵 (서비스 중단 방지)
        if (apiKey == null || apiKey.isBlank()) {
            log.warn("[Gemini] API 키가 설정되지 않아 요약을 건너뜁니다.");
            return null;
        }

        // 너무 짧은 텍스트는 요약 불필요
        if (description == null || description.length() < 50) {
            return null;
        }

        // 토큰 절약: 최대 3,000자만 전달
        String trimmed = description.length() > 3000
                ? description.substring(0, 3000) + "..."
                : description;

        // Gemini API 요청 바디 구조 (JSON)
        // { "contents": [{ "parts": [{ "text": "..." }] }] }
        Map<String, Object> requestBody = Map.of(
                "contents", List.of(
                        Map.of("parts", List.of(
                                Map.of("text",
                                        "다음 채용공고 내용을 200자 이내의 한국어로 요약해주세요. "
                                        + "주요업무, 자격요건, 기술스택 위주로 핵심만 간결하게 작성하세요.\n\n"
                                        + trimmed)
                        ))
                )
        );

        try {
            // @SuppressWarnings: Map<String, Object> 응답을 제네릭 없이 받을 때 발생하는 unchecked 경고 억제
            @SuppressWarnings("unchecked")
            Map<String, Object> response = restClient.post()
                    .uri(GEMINI_URL, model, apiKey)
                    .header("Content-Type", "application/json")
                    .body(requestBody)
                    .retrieve()
                    .body(Map.class);

            // 응답 파싱: candidates[0].content.parts[0].text
            if (response == null) return null;

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> candidates = (List<Map<String, Object>>) response.get("candidates");
            if (candidates == null || candidates.isEmpty()) return null;

            @SuppressWarnings("unchecked")
            Map<String, Object> content = (Map<String, Object>) candidates.get(0).get("content");
            if (content == null) return null;

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> parts = (List<Map<String, Object>>) content.get("parts");
            if (parts == null || parts.isEmpty()) return null;

            String summary = (String) parts.get(0).get("text");
            log.info("[Gemini] 요약 완료 ({}자)", summary != null ? summary.length() : 0);
            return summary;

        } catch (Exception e) {
            log.error("[Gemini] API 호출 실패: {}", e.getMessage());
            return null;
        }
    }
}
