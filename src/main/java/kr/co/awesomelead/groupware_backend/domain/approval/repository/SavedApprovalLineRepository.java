package kr.co.awesomelead.groupware_backend.domain.approval.repository;

import kr.co.awesomelead.groupware_backend.domain.approval.entity.SavedApprovalLine;
import kr.co.awesomelead.groupware_backend.domain.approval.enums.ApprovalSavedLineType;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface SavedApprovalLineRepository extends JpaRepository<SavedApprovalLine, Long> {

    @Query(
            "select distinct l from SavedApprovalLine l "
                    + "left join fetch l.details d "
                    + "left join fetch d.targetUser "
                    + "left join fetch d.targetDepartment "
                    + "where l.ownerUser.id = :ownerUserId "
                    + "and l.lineType = :lineType and l.isActive = true "
                    + "order by l.modifiedAt desc, l.id desc")
    List<SavedApprovalLine> findAllPersonalWithDetails(
            @Param("ownerUserId") Long ownerUserId,
            @Param("lineType") ApprovalSavedLineType lineType);

    @Query(
            "select distinct l from SavedApprovalLine l "
                    + "left join fetch l.details d "
                    + "left join fetch d.targetUser "
                    + "left join fetch d.targetDepartment "
                    + "where l.department.id = :departmentId "
                    + "and l.lineType = :lineType and l.isActive = true "
                    + "order by l.modifiedAt desc, l.id desc")
    List<SavedApprovalLine> findAllDepartmentWithDetails(
            @Param("departmentId") Long departmentId,
            @Param("lineType") ApprovalSavedLineType lineType);

    @Query(
            "select distinct l from SavedApprovalLine l "
                    + "left join fetch l.ownerUser "
                    + "left join fetch l.department "
                    + "left join fetch l.createdByUser "
                    + "left join fetch l.details d "
                    + "left join fetch d.targetUser "
                    + "left join fetch d.targetDepartment "
                    + "where l.id = :id")
    Optional<SavedApprovalLine> findWithDetailsById(@Param("id") Long id);
}
