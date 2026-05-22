package com.jobradar.backend.scrap.service;

import com.jobradar.backend.global.exception.CustomException;
import com.jobradar.backend.global.exception.ErrorCode;
import com.jobradar.backend.job.entity.Job;
import com.jobradar.backend.job.repository.JobRepository;
import com.jobradar.backend.scrap.entity.Scrap;
import com.jobradar.backend.scrap.repository.ScrapRepository;
import com.jobradar.backend.user.entity.User;
import com.jobradar.backend.user.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

/**
 * ScrapService 단위 테스트
 *
 * [테스트 대상 선정 이유]
 * - 중복 스크랩 방지: 같은 공고를 두 번 스크랩하면 예외 발생해야 함 → 핵심 비즈니스 규칙
 * - 권한 검증: 타인의 스크랩을 삭제하려 할 때 403 예외 발생해야 함 → 보안 핵심 로직
 */
@ExtendWith(MockitoExtension.class)
class ScrapServiceTest {

    @Mock
    private ScrapRepository scrapRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private JobRepository jobRepository;

    @InjectMocks
    private ScrapService scrapService;

    // ===== 스크랩 추가 테스트 =====

    @Test
    @DisplayName("스크랩 추가 실패 - 이미 스크랩한 공고 → SCRAP_ALREADY_EXISTS 예외")
    void addScrap_중복스크랩_예외() {
        // given: 해당 사용자가 이미 이 공고를 스크랩한 상태
        given(scrapRepository.existsByUserEmailAndJobId("test@example.com", 1L)).willReturn(true);

        // when & then
        assertThatThrownBy(() -> scrapService.addScrap("test@example.com", 1L))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.SCRAP_ALREADY_EXISTS);
    }

    // ===== 스크랩 삭제 테스트 =====

    @Test
    @DisplayName("스크랩 삭제 실패 - 타인의 스크랩 삭제 시도 → FORBIDDEN 예외")
    void deleteScrap_타인스크랩삭제_예외() {
        // given: 스크랩 소유자는 owner@example.com
        User owner = User.builder()
                .email("owner@example.com")
                .password("password")
                .nickname("소유자")
                .build();

        Job job = Job.builder()
                .company("테스트회사")
                .title("개발자 모집")
                .location("서울")
                .sourceUrl("https://example.com")
                .sourceSite("사람인")
                .build();

        Scrap scrap = Scrap.builder()
                .user(owner)
                .job(job)
                .build();

        given(scrapRepository.findById(1L)).willReturn(Optional.of(scrap));

        // when & then: attacker@example.com이 타인(owner)의 스크랩 삭제 시도
        assertThatThrownBy(() -> scrapService.deleteScrap("attacker@example.com", 1L))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.FORBIDDEN);
    }
}
