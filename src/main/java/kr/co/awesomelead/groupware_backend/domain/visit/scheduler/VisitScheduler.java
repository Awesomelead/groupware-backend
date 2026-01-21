package kr.co.awesomelead.groupware_backend.domain.visit.scheduler;

import java.time.LocalDate;
import java.util.List;
import kr.co.awesomelead.groupware_backend.domain.visit.entity.Visit;
import kr.co.awesomelead.groupware_backend.domain.visit.enums.VisitStatus;
import kr.co.awesomelead.groupware_backend.domain.visit.repository.VisitRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
public class VisitScheduler {

    private final VisitRepository visitRepository;

    @Transactional
    @Scheduled(cron = "0 0 0 * * *")
    public void autoExpireLongTermVisits() {
        log.info("만료된 장기 방문 건 자동 종료 스케줄러 시작: {}", LocalDate.now());

        // 1. 만료 대상 조회 (종료일이 오늘보다 이전인 건)
        List<Visit> expiredVisits = visitRepository.findAllByIsLongTermTrueAndEndDateBeforeAndStatusNot(
            LocalDate.now(),
            VisitStatus.COMPLETED
        );

        if (expiredVisits.isEmpty()) {
            log.info("만료 대상 장기 방문 건이 없습니다.");
            return;
        }

        // 2. 상태 업데이트
        expiredVisits.forEach(visit -> {
            log.info("장기 방문 만료 처리 - ID: {}, 내방객: {}, 종료일: {}",
                visit.getId(), visit.getVisitorName(), visit.getEndDate());

            visit.setStatus(VisitStatus.COMPLETED);
        });

        log.info("총 {}건의 장기 방문이 자동 종료되었습니다.", expiredVisits.size());
    }

}
