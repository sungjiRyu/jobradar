package com.jobradar.backend.global.ai;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

/** Groq 채용공고 AI 요약 서비스 */
@Slf4j
@Service
public class AiSummaryService {

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

    @Value("${ai.api-key:${GROQ_API_KEY:${AI_API_KEY:}}}")
    private String apiKey;

    @Value("${ai.base-url:${AI_BASE_URL:https://api.groq.com/openai/v1/chat/completions}}")
    private String apiUrl;

    @Value("${ai.model:${AI_MODEL:openai/gpt-oss-120b}}")
    private String model;

    private final RestClient restClient = RestClient.create();

    @PostConstruct
    public void init() {
        apiKey = resolveApiKey();
        if (apiKey == null || apiKey.isBlank()) {
            log.warn("[Groq] api-key가 설정되지 않아 AI 요약을 사용할 수 없습니다.");
            return;
        }
        log.info("[Groq] AI 요약 초기화 완료. model={}", model);
    }

    /**
     * 채용공고 description을 받아 구조화된 JSON 문자열 반환
     *
     * @param description 공고 전체 텍스트
     * @return JSON 문자열 (api-key 없거나 실패 시 null)
     */
    public AiSummaryResult summarize(String description) {
        if (apiKey == null || apiKey.isBlank()) {
            log.warn("[Groq] api-key가 설정되지 않아 요약을 건너뜁니다.");
            return AiSummaryResult.failed();
        }
        if (description == null || description.length() < 50) {
            return AiSummaryResult.failed();
        }

        String trimmed = description.length() > 3000
                ? description.substring(0, 3000) + "..."
                : description;

        Map<String, Object> requestBody = Map.of(
                "model", model,
                "messages", List.of(
                        Map.of(
                                "role", "user",
                                "content", PROMPT_TEMPLATE.formatted(trimmed)
                        )
                ),
                "temperature", 0.1
        );

        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> response = restClient.post()
                    .uri(apiUrl)
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + apiKey)
                    .body(requestBody)
                    .retrieve()
                    .body(Map.class);

            if (response == null) return AiSummaryResult.failed();

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> choices = (List<Map<String, Object>>) response.get("choices");
            if (choices == null || choices.isEmpty()) return AiSummaryResult.failed();

            @SuppressWarnings("unchecked")
            Map<String, Object> message = (Map<String, Object>) choices.get(0).get("message");
            if (message == null) return AiSummaryResult.failed();

            String result = (String) message.get("content");
            String json = stripJsonCodeBlock(result);
            log.info("[Groq] JSON 정리 완료. model={}, length={}", model, json != null ? json.length() : 0);
            return json == null ? AiSummaryResult.failed() : AiSummaryResult.success(json);

        } catch (Exception e) {
            log.error("[Groq] API 호출 실패: {}", e.getMessage());
            return AiSummaryResult.failed();
        }
    }

    /**
     * LLM 응답에서 ```json ... ``` 코드블록 래퍼 제거
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

    private String resolveApiKey() {
        if (apiKey != null && !apiKey.isBlank()) {
            return apiKey;
        }

        String groqApiKey = readEnvFileValue("GROQ_API_KEY");
        if (groqApiKey != null && !groqApiKey.isBlank()) {
            return groqApiKey;
        }

        return readEnvFileValue("AI_API_KEY");
    }

    private String readEnvFileValue(String key) {
        for (Path envPath : List.of(Path.of(".env.local"), Path.of("..", ".env.local"))) {
            try {
                if (!Files.isRegularFile(envPath)) {
                    continue;
                }

                for (String line : Files.readAllLines(envPath)) {
                    String trimmed = line.strip();
                    if (trimmed.isEmpty() || trimmed.startsWith("#")) {
                        continue;
                    }

                    int separator = trimmed.indexOf('=');
                    if (separator < 1 || !trimmed.substring(0, separator).strip().equals(key)) {
                        continue;
                    }

                    return stripQuotes(trimmed.substring(separator + 1).strip());
                }
            } catch (IOException e) {
                log.warn("[Groq] .env.local 읽기 실패: {}", e.getMessage());
            }
        }

        return null;
    }

    private String stripQuotes(String value) {
        if ((value.startsWith("\"") && value.endsWith("\""))
                || (value.startsWith("'") && value.endsWith("'"))) {
            return value.substring(1, value.length() - 1);
        }
        return value;
    }
}
