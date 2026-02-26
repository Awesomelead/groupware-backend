package kr.co.awesomelead.groupware_backend.domain.user.repository;

import kr.co.awesomelead.groupware_backend.domain.user.entity.MyInfoUpdateRequest;
import kr.co.awesomelead.groupware_backend.domain.user.enums.MyInfoUpdateRequestStatus;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface MyInfoUpdateRequestRepository extends JpaRepository<MyInfoUpdateRequest, Long> {

    boolean existsByUserIdAndStatus(Long userId, MyInfoUpdateRequestStatus status);

    Optional<MyInfoUpdateRequest> findFirstByUserIdAndStatusOrderByCreatedAtDesc(
            Long userId, MyInfoUpdateRequestStatus status);

    @Query(
            "SELECT r FROM MyInfoUpdateRequest r JOIN FETCH r.user u "
                    + "WHERE r.status = :status ORDER BY r.createdAt DESC")
    List<MyInfoUpdateRequest> findAllByStatusWithUser(
            @Param("status") MyInfoUpdateRequestStatus status);
}
