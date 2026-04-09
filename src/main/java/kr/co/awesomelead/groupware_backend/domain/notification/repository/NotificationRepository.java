package kr.co.awesomelead.groupware_backend.domain.notification.repository;

import kr.co.awesomelead.groupware_backend.domain.notification.entity.Notification;
import kr.co.awesomelead.groupware_backend.domain.notification.enums.NotificationDomainType;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    Page<Notification> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);

    Page<Notification> findByUserIdAndRequiresApprovalTrueOrderByCreatedAtDesc(
            Long userId, Pageable pageable);

    long countByUserIdAndIsReadFalse(Long userId);

    void deleteByDomainTypeAndDomainId(NotificationDomainType domainType, Long domainId);

    @Modifying
    @Query(
            "UPDATE Notification n SET n.requiresApproval = false WHERE n.domainType = :domainType"
                + " AND n.domainId = :domainId AND n.requiresApproval = true")
    void resolveRequiresApprovalByDomainTypeAndDomainId(
            @Param("domainType") NotificationDomainType domainType,
            @Param("domainId") Long domainId);
}
