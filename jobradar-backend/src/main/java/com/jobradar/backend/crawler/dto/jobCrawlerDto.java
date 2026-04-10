package com.jobradar.backend.crawler.dto;

import java.time.LocalDateTime;
import java.util.List;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class jobCrawlerDto {
    
    private String title;           // 공고 제목
    private String company;         // 회사명
    private String location;        // 지역 (예: 서울 강남구)
    private String experience;      // 경력 (예: 신입, 경력 3년 등)
    private String url;             // 채용공고 원본 링크 (클릭 시 이동할 주소)
    private LocalDateTime deadline; // 서류 마감일
    private List<String> techStacks; // 요구 기술 스택 (예: ["Spring Boot", "MySQL"])


}
