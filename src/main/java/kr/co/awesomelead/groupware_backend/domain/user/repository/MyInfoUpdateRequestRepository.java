package kr.co.awesomelead.groupware_backend.domain.user.repository;

import java.util.Optional;
import kr.co.awesomelead.groupware_backend.domain.user.entity.MyInfoUpdateRequest;
import kr.co.awesomelead.groupware_backend.domain.user.enums.MyInfoUpdateRequestStatus;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MyInfoUpdateRequestRepository
    extends JpaRepository<MyInfoUpdateRequest, Long> {

    boolean existsByUserIdAndStatus(Long userId, MyInfoUpdateRequestStatus status);

    Optional<MyInfoUpdateRequest> findFirstByUserIdAndStatusOrderByCreatedAtDesc(
        Long userId, MyInfoUpdateRequestStatus status);
}
