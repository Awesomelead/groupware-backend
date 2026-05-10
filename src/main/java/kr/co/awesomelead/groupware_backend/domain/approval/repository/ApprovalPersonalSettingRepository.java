package kr.co.awesomelead.groupware_backend.domain.approval.repository;

import kr.co.awesomelead.groupware_backend.domain.approval.entity.ApprovalPersonalSetting;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface ApprovalPersonalSettingRepository
        extends JpaRepository<ApprovalPersonalSetting, Long> {

    Optional<ApprovalPersonalSetting> findByUserId(Long userId);

    @Query(
            "select distinct s from ApprovalPersonalSetting s "
                    + "left join fetch s.delegateUser "
                    + "left join fetch s.defaultViewerTargets t "
                    + "left join fetch t.targetUser "
                    + "left join fetch t.targetDepartment "
                    + "where s.user.id = :userId")
    Optional<ApprovalPersonalSetting> findByUserIdWithTargets(@Param("userId") Long userId);
}

