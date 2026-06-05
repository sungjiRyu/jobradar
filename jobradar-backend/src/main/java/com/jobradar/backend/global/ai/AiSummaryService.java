package com.jobradar.backend.global.ai;

import com.google.auth.oauth2.GoogleCredentials;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.List;
import java.util.Map;

/** Vertex AI (Gemini) 채용공고 AI 요약 서비스 */
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

    @Value("${vertex.project-id:}")
    private String projectId;

    @Value("${vertex.location:us-central1}")
    private String location;

    @Value("${vertex.model:gemini-2.5-flash}")
    private String model;

    @Value("${vertex.credentials-path:}")
    private String credentialsPath;

    private GoogleCredentials credentials;
    private String vertexUrl;

    private final RestClient restClient = RestClient.create();

    /**
     * 서비스 계정 JSON으로 GoogleCredentials 초기화
     * credentials-path가 없으면 warn 로그만 남기고 건너뜀 (요약 기능 비활성화)
     */
    @PostConstruct
    public void init() {
        if (credentialsPath == null || credentialsPath.isBlank()) {
            log.warn("[Vertex] credentials-path가 설정되지 않아 AI 요약을 사용할 수 없습니다.");
            return;
        }
        try {
            credentials = GoogleCredentials
                    .fromStream(new FileInputStream(credentialsPath))
                    .createScoped("https://www.googleapis.com/auth/cloud-platform");
            vertexUrl = String.format(
                    "https://%s-aiplatform.googleapis.com/v1/projects/%s/locations/%s/publishers/google/models/%s:generateContent",
                    location, projectId, location, model);
            log.info("[Vertex] 서비스 계정 인증 초기화 완료. url={}", vertexUrl);
        } catch (IOException e) {
            log.error("[Vertex] 서비스 계정 JSON 로드 실패: {}", e.getMessage());
        }
    }

    /**
     * 채용공고 description을 받아 구조화된 JSON 문자열 반환
     *
     * @param description 공고 전체 텍스트
     * @return JSON 문자열 (credentials 없거나 실패 시 null)
     */
    public String summarize(String description) {
        if (credentials == null) {
            log.warn("[Vertex] credentials가 초기화되지 않아 요약을 건너뜁니다.");
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
                        Map.of(
                                "role", "user",
                                "parts", List.of(Map.of("text", PROMPT_TEMPLATE.formatted(trimmed)))
                        )
                )
        );

        try {
            // 토큰 만료 시 자동 갱신 후 Bearer 헤더에 첨부
            credentials.refreshIfExpired();
            String token = credentials.getAccessToken().getTokenValue();

            @SuppressWarnings("unchecked")
            Map<String, Object> response = restClient.post()
                    .uri(vertexUrl)
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + token)
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
            String json = stripJsonCodeBlock(result);
            log.info("[Vertex] JSON 정리 완료 ({}자)", json != null ? json.length() : 0);
            return json;

        } catch (Exception e) {
            log.error("[Vertex] API 호출 실패: {}", e.getMessage());
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
