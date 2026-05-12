package com.jobradar.backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * @EnableScheduling
 * - Spring의 @Scheduled 어노테이션을 활성화하는 설정
 * - 이 어노테이션이 없으면 @Scheduled(cron = "...")을 붙여도 스케줄러가 실행되지 않음
 *
 * @EnableAsync
 * - @Async 어노테이션을 활성화하는 설정
 * - 백필 API가 HTTP 요청 스레드를 막지 않고 백그라운드에서 실행되도록 사용
 */
@SpringBootApplication
@EnableScheduling
@EnableAsync
public class JobradarBackendApplication {

	public static void main(String[] args) {
		SpringApplication.run(JobradarBackendApplication.class, args);
	}

}
