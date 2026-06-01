package kr.co.awesomelead.groupware_backend.domain.annualleave.repository;

import kr.co.awesomelead.groupware_backend.domain.annualleave.entity.AnnualLeaveDispatchHistory;
import kr.co.awesomelead.groupware_backend.domain.department.enums.Company;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface AnnualLeaveDispatchHistoryRepository
        extends JpaRepository<AnnualLeaveDispatchHistory, Long> {

    @Query("SELECT h FROM AnnualLeaveDispatchHistory h ORDER BY h.createdAt DESC, h.id DESC")
    List<AnnualLeaveDispatchHistory> findAllOrderByCreatedAtDesc();

    @Query(
            "SELECT h FROM AnnualLeaveDispatchHistory h WHERE (:company IS NULL OR h.company ="
                + " :company) ORDER BY h.createdAt DESC, h.id DESC")
    List<AnnualLeaveDispatchHistory> findAllByCompanyOrderByCreatedAtDesc(
            @Param("company") Company company);

    boolean existsByBaseDateBetween(LocalDate start, LocalDate end);

    boolean existsByIdNotAndBaseDateBetween(Long id, LocalDate start, LocalDate end);

    boolean existsByCompanyAndBaseDateBetween(Company company, LocalDate start, LocalDate end);

    boolean existsByIdNotAndCompanyAndBaseDateBetween(
            Long id, Company company, LocalDate start, LocalDate end);
}
