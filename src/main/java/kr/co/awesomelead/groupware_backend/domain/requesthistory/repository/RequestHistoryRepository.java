package kr.co.awesomelead.groupware_backend.domain.requesthistory.repository;

import kr.co.awesomelead.groupware_backend.domain.requesthistory.entity.RequestHistory;
import kr.co.awesomelead.groupware_backend.domain.requesthistory.enums.RequestHistoryStatus;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface RequestHistoryRepository extends JpaRepository<RequestHistory, Long> {

    List<RequestHistory> findByUserIdOrderByRequestDateDescIdDesc(Long userId);

    Optional<RequestHistory> findByIdAndUserId(Long id, Long userId);

    @Query(
            "select rh from RequestHistory rh "
                    + "join fetch rh.user u "
                    + "left join fetch u.department d "
                    + "where rh.id = :id")
    Optional<RequestHistory> findByIdWithUserAndDepartment(@Param("id") Long id);

    @Query(
            value =
                    "select rh from RequestHistory rh "
                            + "join fetch rh.user u "
                            + "left join fetch u.department d "
                            + "where (:status is null or rh.approvalStatus = :status)",
            countQuery =
                    "select count(rh) from RequestHistory rh "
                            + "where (:status is null or rh.approvalStatus = :status)")
    Page<RequestHistory> findAllWithUserAndDepartmentByStatus(
            @Param("status") RequestHistoryStatus status, Pageable pageable);
}
