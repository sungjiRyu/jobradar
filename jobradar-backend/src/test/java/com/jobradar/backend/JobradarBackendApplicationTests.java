package com.jobradar.backend;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

// 전체 Spring 컨텍스트 로딩 테스트 — MySQL + Redis Docker 컨테이너가 실행 중일 때만 동작
// 단위 테스트(./gradlew test)와 분리하여 인프라 없이도 빌드 가능하게 처리
@Disabled("통합 테스트: docker-compose up 후 실행")
@SpringBootTest
class JobradarBackendApplicationTests {

	@Test
	void contextLoads() {
	}

}
