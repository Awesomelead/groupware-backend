package kr.co.awesomelead.groupware_backend.domain.approval.repository.querydsl;

import static kr.co.awesomelead.groupware_backend.domain.approval.entity.QApproval.approval;

import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;

import kr.co.awesomelead.groupware_backend.domain.approval.dto.request.ApprovalListRequestDto;
import kr.co.awesomelead.groupware_backend.domain.approval.dto.response.ApprovalSummaryResponseDto;
import kr.co.awesomelead.groupware_backend.domain.approval.entity.Approval;
import kr.co.awesomelead.groupware_backend.domain.approval.entity.document.BasicApproval;
import kr.co.awesomelead.groupware_backend.domain.approval.entity.document.CarFuelApproval;
import kr.co.awesomelead.groupware_backend.domain.approval.entity.document.ExpenseDraftApproval;
import kr.co.awesomelead.groupware_backend.domain.approval.entity.document.LeaveApproval;
import kr.co.awesomelead.groupware_backend.domain.approval.entity.document.OverseasTripApproval;
import kr.co.awesomelead.groupware_backend.domain.approval.entity.document.WelfareExpenseApproval;
import kr.co.awesomelead.groupware_backend.domain.approval.enums.ApprovalCategory;
import kr.co.awesomelead.groupware_backend.domain.approval.enums.ApprovalStatus;
import kr.co.awesomelead.groupware_backend.domain.approval.enums.DocumentType;
import kr.co.awesomelead.groupware_backend.domain.approval.enums.ParticipantType;
import kr.co.awesomelead.groupware_backend.domain.approval.mapper.ApprovalMapper;
import kr.co.awesomelead.groupware_backend.domain.user.enums.Role;

import lombok.RequiredArgsConstructor;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Repository
@RequiredArgsConstructor
public class ApprovalQueryRepository {

    private final JPAQueryFactory queryFactory;
    private final ApprovalMapper approvalMapper;

    public Page<ApprovalSummaryResponseDto> findApprovalsByCondition(
            ApprovalListRequestDto condition, Long userId, String userRole) {
        Pageable pageable = PageRequest.of(condition.getPage(), condition.getSize());

        // 1. 카테고리별 기본 조회 조건 (ALL, IN_PROGRESS, REFERENCE, DRAFT)
        BooleanExpression categoryExpression = getCategoryExpression(condition, userId, userRole);

        // 2. 문서 양식 종류(DocumentType) 필터
        BooleanExpression docTypeExpression = eqDocumentType(condition.getDocumentType());

        // 3. 메인 쿼리 작성 (N+1 최적화를 위해 컬렉션은 지연 로딩 후 application 단에서 가공하거나, 엔티티로 한번에 로딩)
        JPAQuery<Approval> query = queryFactory
                .selectFrom(approval)
                .where(categoryExpression, docTypeExpression)
                .orderBy(approval.createdAt.desc())
                .distinct();

        // 4. 페이징 적용된 엔티티 조회
        List<Approval> approvals = query.offset(pageable.getOffset()).limit(pageable.getPageSize()).fetch();

        // 5. 전체 카운트 조회
        long totalCount = Optional.ofNullable(
                queryFactory
                        .select(approval.countDistinct())
                        .from(approval)
                        .where(categoryExpression, docTypeExpression)
                        .fetchOne())
                .orElse(0L);

        // 6. DTO 변환
        return new PageImpl<>(
                approvals.stream()
                        .map(approval -> approvalMapper.toSummaryResponseDto(approval, userId))
                        .collect(Collectors.toList()),
                pageable,
                totalCount == 0L ? 0 : totalCount);
    }

    /** 카테고리(ALL, IN_PROGRESS, REFERENCE, DRAFT) 및 Status 하위 필터에 따른 동적 쿼리 조합 */
    private BooleanExpression getCategoryExpression(
            ApprovalListRequestDto condition, Long userId, String userRole) {
        ApprovalCategory category = condition.getCategory();
        if (category == null) {
            return null;
        }

        return switch (category) {
            case ALL -> getAllCategoryExpression(userId, userRole);
            case IN_PROGRESS -> getInProgressCategoryExpression(condition.getStatus(), userId);
            case REFERENCE -> getReferenceCategoryExpression(
                    condition.getStatus(), condition.getParticipantType(), userId);
            case DRAFT -> getDraftCategoryExpression(condition.getStatus(), userId);
        };
    }

    /**
     * 1. ALL (전체) 카테고리 조건 - ADMIN/MASTER_ADMIN 이면 모든 문서 조회 - USER 이면 (결재진행 + 참조 +
     * 내작성) 전체 합집합
     */
    private BooleanExpression getAllCategoryExpression(Long userId, String userRole) {
        if (Role.ADMIN.name().equals(userRole) || Role.MASTER_ADMIN.name().equals(userRole)) {
            return null; // 조건 없이 전체 풀 스캔
        }

        // 일반 유저인 경우 연관된 모든 문서 (기안자이거나, 결재선에 포함되거나, 참조자에 포함됨)
        return approval.drafter.id
                .eq(userId) // 내 작성
                .or(approval.steps.any().approver.id.eq(userId)) // 결재 진행 연관
                .or(approval.participants.any().user.id.eq(userId)); // 참조/열람 연관
    }

    /** 2. IN_PROGRESS (결재 진행) 카테고리 조건 */
    private BooleanExpression getInProgressCategoryExpression(ApprovalStatus status, Long userId) {
        // 기본적으로 내가 결재선에 포함되어 있어야 함
        BooleanExpression baseCondition = approval.steps.any().approver.id.eq(userId);

        if (status == null) {
            return baseCondition;
        }

        return switch (status) {
            case WAITING -> baseCondition.and(
                    approval.steps
                            .any().approver.id
                            .eq(userId)
                            .and(approval.steps.any().status.eq(ApprovalStatus.PENDING))); // 내 차례
            case APPROVED -> baseCondition.and(
                    approval.steps
                            .any().approver.id
                            .eq(userId)
                            .and(
                                    approval.steps
                                            .any().status
                                            .eq(ApprovalStatus.APPROVED))); // 내가 기결함
            case REJECTED -> baseCondition.and(
                    // 내 단계가 반려거나, 혹은 다른 단계에서 반려/회수되어 문서가 결국 반려/취소 상태인 경우
                    approval.steps
                            .any().approver.id
                            .eq(userId)
                            .and(approval.steps.any().status.eq(ApprovalStatus.REJECTED))
                            .or(
                                    approval.status.in(
                                            ApprovalStatus.REJECTED, ApprovalStatus.CANCELED)));
            default -> baseCondition;
        };
    }

    /** 3. REFERENCE (참조 문서) 카테고리 조건 */
    private BooleanExpression getReferenceCategoryExpression(
            ApprovalStatus status, ParticipantType participantType, Long userId) {
        // 1. 참조자(REFERRER)인 경우: 상신 직후부터 전체 노출
        BooleanExpression isReferrer = approval.participants
                .any().user.id
                .eq(userId)
                .and(
                        approval.participants
                                .any().participantType
                                .eq(ParticipantType.REFERRER));

        // 2. 열람권자(VIEWER)인 경우: 최종 승인(APPROVED)된 문서만 노출
        BooleanExpression isViewerAndApproved = approval.participants
                .any().user.id
                .eq(userId)
                .and(approval.participants.any().participantType.eq(ParticipantType.VIEWER))
                .and(approval.status.eq(ApprovalStatus.APPROVED));

        // ROLE(ParticipantType) 필터가 명시된 경우 해당 역할만 필터링
        if (participantType == ParticipantType.REFERRER) {
            return isReferrer;
        }
        if (participantType == ParticipantType.VIEWER) {
            return isViewerAndApproved;
        }

        // 기본 합집합 조건
        BooleanExpression baseCondition = isReferrer.or(isViewerAndApproved);

        // 상태값(APPROVED, REJECTED 등)이 명시된 경우 추가 필터링
        if (status != null) {
            return baseCondition.and(approval.status.eq(status));
        }

        return baseCondition;
    }

    /** 4. DRAFT (내 작성) 카테고리 조건 */
    private BooleanExpression getDraftCategoryExpression(ApprovalStatus status, Long userId) {
        BooleanExpression baseCondition = approval.drafter.id.eq(userId);

        if (status == null) {
            return baseCondition;
        }

        // 내 작성 문서에서의 상태 필터링
        return switch (status) {
            case WAITING, PENDING, IN_PROGRESS -> baseCondition.and(
                    approval.status.in(ApprovalStatus.WAITING, ApprovalStatus.PENDING));
            case APPROVED -> baseCondition.and(approval.status.eq(ApprovalStatus.APPROVED));
            case REJECTED, CANCELED -> baseCondition.and(
                    approval.status.in(ApprovalStatus.REJECTED, ApprovalStatus.CANCELED));
        };
    }

    /** 서식(문서 종류) 서브 필터 */
    private BooleanExpression eqDocumentType(DocumentType documentType) {
        if (documentType == null) {
            return null;
        }
        return switch (documentType) {
            case BASIC -> approval.instanceOf(BasicApproval.class);
            case LEAVE -> approval.instanceOf(LeaveApproval.class);
            case OVERSEAS_TRIP -> approval.instanceOf(OverseasTripApproval.class);
            case EXPENSE_DRAFT -> approval.instanceOf(ExpenseDraftApproval.class);
            case WELFARE_EXPENSE -> approval.instanceOf(WelfareExpenseApproval.class);
            case CAR_FUEL -> approval.instanceOf(CarFuelApproval.class);
        };
    }
}
