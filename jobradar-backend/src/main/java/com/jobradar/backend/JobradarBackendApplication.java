package com.jobradar.backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * @EnableScheduling
 * - Spring의 @Scheduled 어노테이션을 활성화하는 설정
 * - 이 어노테이션이 없으면 @Scheduled(cron = "...")을 붙여도 스케줄러가 실행되지 않음
 * - @SpringBootApplication이 있는 메인 클래스에 붙이는 것이 관례
 */
@SpringBootApplication
@EnableScheduling
public class JobradarBackendApplication {

	public static void main(String[] args) {
		SpringApplication.run(JobradarBackendApplication.class, args);
	}

}
