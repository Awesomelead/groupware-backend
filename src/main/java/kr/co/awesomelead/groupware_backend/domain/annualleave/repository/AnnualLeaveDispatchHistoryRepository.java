package kr.co.awesomelead.groupware_backend.domain.annualleave.repository;

import kr.co.awesomelead.groupware_backend.domain.annualleave.entity.AnnualLeaveDispatchHistory;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDate;
import java.util.List;

public interface AnnualLeaveDispatchHistoryRepository
        extends JpaRepository<AnnualLeaveDispatchHistory, Long> {

    @Query("SELECT h FROM AnnualLeaveDispatchHistory h ORDER BY h.createdAt DESC, h.id DESC")
    List<AnnualLeaveDispatchHistory> findAllOrderByCreatedAtDesc();

    boolean existsByBaseDateBetween(LocalDate start, LocalDate end);

    boolean existsByIdNotAndBaseDateBetween(Long id, LocalDate start, LocalDate end);
}
