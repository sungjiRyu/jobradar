package com.jobradar.backend.scrap.service;

import com.jobradar.backend.global.exception.CustomException;
import com.jobradar.backend.global.exception.ErrorCode;
import com.jobradar.backend.job.entity.Job;
import com.jobradar.backend.job.repository.JobRepository;
import com.jobradar.backend.scrap.dto.ScrapResponse;
import com.jobradar.backend.scrap.entity.Scrap;
import com.jobradar.backend.scrap.repository.ScrapRepository;
import com.jobradar.backend.user.entity.User;
import com.jobradar.backend.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 스크랩 서비스
 *
 * 비즈니스 로직을 담당하며, Controller와 Repository 사이의 중간 계층이다.
 * @Transactional: 메서드 실행을 하나의 트랜잭션으로 묶어 데이터 정합성을 보장
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true) // 기본 읽기 전용 → 쓰기 메서드에만 @Transactional 오버라이드
public class ScrapService {

    private final ScrapRepository scrapRepository;
    private final UserRepository userRepository;
    private final JobRepository jobRepository;

    /**
     * 스크랩 추가
     *
     * 1. 이미 스크랩한 공고인지 중복 체크
     * 2. User, Job 엔티티 조회 후 Scrap 생성
     * 3. 기본 상태: PENDING (지원예정)
     *
     * @Transactional: 데이터를 변경(INSERT)하므로 readOnly = false
     */
    @Transactional
    public ScrapResponse addScrap(String email, Long jobPostId) {
        // 중복 스크랩 체크
        if (scrapRepository.existsByUserEmailAndJobId(email, jobPostId)) {
            throw new CustomException(ErrorCode.SCRAP_ALREADY_EXISTS);
        }

        // 사용자 조회
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        // 채용공고 조회
        Job job = jobRepository.findById(jobPostId)
                .orElseThrow(() -> new CustomException(ErrorCode.JOB_NOT_FOUND));

        // 마감된 공고는 신규 스크랩 불가 — 기존 스크랩은 마이페이지에 그대로 노출됨
        if (job.getStatus() == Job.JobStatus.CLOSED
                || (job.getDeadline() != null && job.getDeadline().isBefore(LocalDate.now()))) {
            throw new CustomException(ErrorCode.SCRAP_CLOSED_JOB);
        }

        // 스크랩 생성 및 저장 (Builder 패턴 사용, 기본 상태 PENDING)
        Scrap scrap = Scrap.builder()
                .user(user)
                .job(job)
                .build();

        scrapRepository.save(scrap);

        return ScrapResponse.from(scrap);
    }

    /**
     * 스크랩 삭제
     *
     * 본인의 스크랩인지 검증 후 삭제
     * → 타인의 스크랩 삭제 시도 시 403 Forbidden
     */
    @Transactional
    public void deleteScrap(String email, Long scrapId) {
        Scrap scrap = findScrapOrThrow(scrapId);
        validateOwner(scrap, email);

        scrapRepository.delete(scrap);
    }

    /**
     * 내 스크랩 목록 조회
     *
     * status 파라미터가 있으면 해당 상태만 필터링, 없으면 전체 조회
     * JOIN FETCH로 N+1 문제 방지
     */
    public List<ScrapResponse> getMyScrapList(String email, Scrap.ScrapStatus status) {
        List<Scrap> scraps;

        if (status != null) {
            // 상태별 필터 조회 (예: PENDING만 보기)
            scraps = scrapRepository.findAllByUserEmailAndStatusWithJob(email, status);
        } else {
            // 전체 조회
            scraps = scrapRepository.findAllByUserEmailWithJob(email);
        }

        // Entity 리스트 → DTO 리스트 변환
        return scraps.stream()
                .map(ScrapResponse::from)
                .collect(Collectors.toList());
    }

    /**
     * 스크랩 상태 변경
     *
     * 더티 체킹(Dirty Checking) 활용:
     * → 영속성 컨텍스트가 관리하는 엔티티의 필드를 변경하면
     *   트랜잭션 종료 시 JPA가 자동으로 UPDATE 쿼리를 실행한다.
     *   (별도로 save()를 호출하지 않아도 됨)
     */
    @Transactional
    public ScrapResponse updateStatus(String email, Long scrapId, Scrap.ScrapStatus newStatus) {
        Scrap scrap = findScrapOrThrow(scrapId);
        validateOwner(scrap, email);

        // 더티 체킹으로 상태 변경 → 트랜잭션 종료 시 자동 UPDATE
        scrap.updateStatus(newStatus);

        return ScrapResponse.from(scrap);
    }

    // ===== Private 헬퍼 메서드 =====

    /** 스크랩 ID로 조회, 없으면 예외 발생 */
    private Scrap findScrapOrThrow(Long scrapId) {
        return scrapRepository.findById(scrapId)
                .orElseThrow(() -> new CustomException(ErrorCode.SCRAP_NOT_FOUND));
    }

    /**
     * 스크랩 소유자 검증
     * 로그인한 사용자의 email과 스크랩 소유자의 email을 비교
     * → 불일치 시 403 Forbidden
     */
    private void validateOwner(Scrap scrap, String email) {
        if (!scrap.getUser().getEmail().equals(email)) {
            throw new CustomException(ErrorCode.FORBIDDEN);
        }
    }
}
