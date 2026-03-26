package kr.co.awesomelead.groupware_backend.domain.safetytraining.repository;

import kr.co.awesomelead.groupware_backend.domain.department.enums.Company;
import kr.co.awesomelead.groupware_backend.domain.safetytraining.entity.SafetyTrainingSession;
import kr.co.awesomelead.groupware_backend.domain.safetytraining.enums.SafetyEducationType;
import kr.co.awesomelead.groupware_backend.domain.safetytraining.enums.SafetyTrainingSessionStatus;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SafetyTrainingSessionRepository
        extends JpaRepository<SafetyTrainingSession, Long> {

    @Query(
            """
            SELECT s
            FROM SafetyTrainingSession s
            WHERE (:companyScope IS NULL OR s.companyScope = :companyScope)
              AND (:educationType IS NULL OR s.educationType = :educationType)
              AND (:status IS NULL OR s.status = :status)
              AND (:startAtFrom IS NULL OR s.startAt >= :startAtFrom)
              AND (:startAtTo IS NULL OR s.startAt <= :startAtTo)
            """)
    Page<SafetyTrainingSession> findAllByFilters(
            @Param("companyScope") Company companyScope,
            @Param("educationType") SafetyEducationType educationType,
            @Param("status") SafetyTrainingSessionStatus status,
            @Param("startAtFrom") java.time.LocalDateTime startAtFrom,
            @Param("startAtTo") java.time.LocalDateTime startAtTo,
            Pageable pageable);
}
