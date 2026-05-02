package com.jobradar.backend.global.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

/** Google Gemini API를 사용한 채용공고 AI 정리 서비스 */
@Slf4j
@Service
public class AiSummaryService {

    private static final String GEMINI_URL =
            "https://generativelanguage.googleapis.com/v1beta/models/{model}:generateContent?key={key}";

    private static final String PROMPT_TEMPLATE = """
            # Role: Senior Dev & Tech Recruiter
            # Task: Parse a Korean IT job posting and return STRICT JSON.

            # Input format notes:
            - Lines matching "선택 : A > B > C" pattern are UI navigation noise — IGNORE completely.
            - Key sections to extract: tasks, required skills, qualifications, preferred qualifications, work conditions.
            - Skills may appear as space/comma-separated lists without delimiters.

            # Constraints:
            1. Output ONLY valid JSON. No extra text.
            2. If info is missing, use [] or null.
            3. All string values in Korean, except dev terms/proper nouns (e.g., 'React', 'AWS', 'CI/CD').

            # JSON Structure:
            {
              "header": { "summary": "One-line dev-focused catchphrase in Korean" },
              "stacks": { "core": ["languages/frameworks"], "infra": ["cloud/db"], "tools": ["dev-ops/collaboration tools"] },
              "details": { "tasks": ["core duties"], "reqs": ["mandatory qualifications"], "pref": ["preferred qualifications"] },
              "conditions": { "type": "employment type", "location": "work location", "salary": "salary in Korean 만원 unit with thousand comma separator (e.g. '3,000만원', '3,000~4,000만원') or null" },
              "culture": ["company culture keywords"],
              "insight": { "challenge": "anticipated technical challenge or growth point", "fit": "best suited developer profile" }
            }

            # Input (plain text):
            %s
            """;

    @Value("${gemini.api-key:}")
    private String apiKey;

    @Value("${gemini.model:}")
    private String model;

    private final RestClient restClient = RestClient.create();

    /**
     * 채용공고 description을 받아 구조화된 JSON 문자열 반환
     *
     * @param description 공고 전체 텍스트
     * @return JSON 문자열 (API 키 없거나 실패 시 null)
     */
    public String summarize(String description) {
        if (apiKey == null || apiKey.isBlank()) {
            log.warn("[Gemini] API 키가 설정되지 않아 요약을 건너뜁니다.");
            return null;
        }
        if (description == null || description.length() < 50) {
            return null;
        }

        String trimmed = description.length() > 3000
                ? description.substring(0, 3000) + "..."
                : description;

        Map<String, Object> requestBody = Map.of(
                "contents", List.of(
                        Map.of("parts", List.of(
                                Map.of("text", PROMPT_TEMPLATE.formatted(trimmed))
                        ))
                )
        );

        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> response = restClient.post()
                    .uri(GEMINI_URL, model, apiKey)
                    .header("Content-Type", "application/json")
                    .body(requestBody)
                    .retrieve()
                    .body(Map.class);

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

            String result = (String) parts.get(0).get("text");

            // LLM이 ```json ... ``` 마크다운 코드블록으로 감싸는 경우 제거
            String json = stripJsonCodeBlock(result);
            log.info("[Gemini] JSON 정리 완료 ({}자)", json != null ? json.length() : 0);
            return json;

        } catch (Exception e) {
            log.error("[Gemini] API 호출 실패: {}", e.getMessage());
            return null;
        }
    }

    /**
     * LLM 응답에서 ```json ... ``` 코드블록 래퍼 제거
     * Gemini가 순수 JSON 대신 마크다운으로 감싸서 반환하는 경우 대응
     */
    private String stripJsonCodeBlock(String text) {
        if (text == null) return null;
        String trimmed = text.strip();
        if (trimmed.startsWith("```")) {
            int start = trimmed.indexOf('\n') + 1;
            int end = trimmed.lastIndexOf("```");
            if (start > 0 && end > start) {
                return trimmed.substring(start, end).strip();
            }
        }
        return trimmed;
    }
}
