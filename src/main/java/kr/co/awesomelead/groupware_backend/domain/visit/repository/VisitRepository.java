package kr.co.awesomelead.groupware_backend.domain.visit.repository;

import kr.co.awesomelead.groupware_backend.domain.visit.entity.Visit;
import kr.co.awesomelead.groupware_backend.domain.visit.enums.VisitStatus;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface VisitRepository extends JpaRepository<Visit, Long> {

    // 1. 특정 부서 필터링 (진형님이 제공해주신 코드)
    @Query(
            "SELECT v FROM Visit v "
                    + "JOIN FETCH v.user u "
                    + "JOIN FETCH u.department d "
                    + "WHERE d.id = :departmentId")
    List<Visit> findAllByDepartmentId(@Param("departmentId") Long departmentId);

    // 2. 전체 조회 시에도 N+1 방지를 위해 Fetch Join 사용
    @Query("SELECT v FROM Visit v " + "JOIN FETCH v.user u " + "JOIN FETCH u.department d")
    List<Visit> findAllWithUserAndDepartment();

    @Query(
            "SELECT v FROM Visit v "
                    + "JOIN FETCH v.user u "
                    + "JOIN FETCH u.department d "
                    + "WHERE (:departmentId IS NULL OR d.id = :departmentId) "
                    + "AND (:status IS NULL OR v.status = :status)")
    List<Visit> findAllByFilters(
            @Param("departmentId") Long departmentId, @Param("status") VisitStatus status);

    List<Visit> findByVisitorNameAndPhoneNumberHash(String name, String inputPhoneHash);

    List<Visit> findAllByIsLongTermTrueAndEndDateBeforeAndStatusNot(
            LocalDate date, VisitStatus status);
}
