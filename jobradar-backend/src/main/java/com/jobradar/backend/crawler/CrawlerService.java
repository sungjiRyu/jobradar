package com.jobradar.backend.crawler;

/**
 * 크롤러 서비스 인터페이스
 *
 * [왜 인터페이스로 만들었는가?]
 * 1. 확장 가능한 구조 (OCP - Open/Closed Principle)
 *    - 현재: SaraminCrawlerService
 *    - 향후: JobkoreaCrawlerService, WantedCrawlerService 등 추가 가능
 *    - 새 사이트 추가 시 이 인터페이스를 구현하는 클래스만 만들면 됨
 *    - 기존 코드 수정 없이 기능 확장 가능
 *
 * 2. 의존성 역전 원칙 (DIP)
 *    - CrawlerScheduler는 구체 클래스가 아닌 이 인터페이스에 의존
 *    - List<CrawlerService>로 모든 구현체를 자동 주입받아 순회 실행
 *    - 면접 포인트: "다형성을 활용해 확장에 열려있고 변경에 닫힌 구조로 설계했습니다"
 */
public interface CrawlerService {

    /**
     * 채용공고를 수집하여 DB에 저장하는 메서드
     * - 각 사이트 크롤러는 이 메서드 하나를 구현하면 됨
     * - 크롤링 실패 시 예외가 외부로 전파되지 않도록 내부에서 처리할 것을 권장
     */
    void collect();

    /**
     * 크롤링 사이트명 반환
     * - 로그 출력, 공고 sourceSite 필드 저장에 사용
     */
    String getSiteName();
}
