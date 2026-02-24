package kr.co.awesomelead.groupware_backend.domain.approval.mapper;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import kr.co.awesomelead.groupware_backend.domain.approval.dto.request.ApprovalCreateRequestDto;
import kr.co.awesomelead.groupware_backend.domain.approval.dto.request.BasicApprovalCreateRequestDto;
import kr.co.awesomelead.groupware_backend.domain.approval.dto.request.CarFuelApprovalCreateRequestDto;
import kr.co.awesomelead.groupware_backend.domain.approval.dto.request.ExpenseDraftApprovalCreateRequestDto;
import kr.co.awesomelead.groupware_backend.domain.approval.dto.request.LeaveApprovalCreateRequestDto;
import kr.co.awesomelead.groupware_backend.domain.approval.dto.request.OverseasTripApprovalCreateRequestDto;
import kr.co.awesomelead.groupware_backend.domain.approval.dto.request.WelfareExpenseApprovalCreateRequestDto;
import kr.co.awesomelead.groupware_backend.domain.approval.dto.response.ApprovalDetailResponseDto;
import kr.co.awesomelead.groupware_backend.domain.approval.dto.response.ApprovalSummaryResponseDto;
import kr.co.awesomelead.groupware_backend.domain.approval.entity.Approval;
import kr.co.awesomelead.groupware_backend.domain.approval.entity.ApprovalAttachment;
import kr.co.awesomelead.groupware_backend.domain.approval.entity.ApprovalParticipant;
import kr.co.awesomelead.groupware_backend.domain.approval.entity.ApprovalStep;
import kr.co.awesomelead.groupware_backend.domain.approval.entity.document.BasicApproval;
import kr.co.awesomelead.groupware_backend.domain.approval.entity.document.CarFuelApproval;
import kr.co.awesomelead.groupware_backend.domain.approval.entity.document.ExpenseDraftApproval;
import kr.co.awesomelead.groupware_backend.domain.approval.entity.document.LeaveApproval;
import kr.co.awesomelead.groupware_backend.domain.approval.entity.document.OverseasTripApproval;
import kr.co.awesomelead.groupware_backend.domain.approval.entity.document.WelfareExpenseApproval;
import kr.co.awesomelead.groupware_backend.global.infra.s3.service.S3Service;
import org.mapstruct.AfterMapping;
import org.mapstruct.BeanMapping;
import org.mapstruct.Context;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.ReportingPolicy;
import org.mapstruct.SubclassExhaustiveStrategy;
import org.mapstruct.SubclassMapping;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface ApprovalMapper {

    @BeanMapping(subclassExhaustiveStrategy = SubclassExhaustiveStrategy.RUNTIME_EXCEPTION)
    @SubclassMapping(source = LeaveApprovalCreateRequestDto.class, target = LeaveApproval.class)
    @SubclassMapping(source = CarFuelApprovalCreateRequestDto.class, target = CarFuelApproval.class)
    @SubclassMapping(source = WelfareExpenseApprovalCreateRequestDto.class, target = WelfareExpenseApproval.class)
    @SubclassMapping(source = ExpenseDraftApprovalCreateRequestDto.class, target = ExpenseDraftApproval.class)
    @SubclassMapping(source = OverseasTripApprovalCreateRequestDto.class, target = OverseasTripApproval.class)
    @SubclassMapping(source = BasicApprovalCreateRequestDto.class, target = BasicApproval.class)
    @Mapping(target = "retentionPeriod", ignore = true)
    Approval toEntity(ApprovalCreateRequestDto dto);

    @Mapping(target = "drafter", ignore = true)
    @Mapping(target = "draftDepartment", ignore = true)
    @Mapping(target = "participants", ignore = true)
    @Mapping(target = "attachments", ignore = true)
    @Mapping(target = "steps", ignore = true)
    @Mapping(target = "retentionPeriod", ignore = true)
    LeaveApproval toLeaveEntity(LeaveApprovalCreateRequestDto dto);

    @Mapping(target = "drafter", ignore = true)
    @Mapping(target = "draftDepartment", ignore = true)
    @Mapping(target = "participants", ignore = true)
    @Mapping(target = "attachments", ignore = true)
    @Mapping(target = "steps", ignore = true)
    @Mapping(target = "retentionPeriod", ignore = true)
    CarFuelApproval toCarFuelEntity(CarFuelApprovalCreateRequestDto dto);

    @Mapping(target = "drafter", ignore = true)
    @Mapping(target = "draftDepartment", ignore = true)
    @Mapping(target = "participants", ignore = true)
    @Mapping(target = "attachments", ignore = true)
    @Mapping(target = "steps", ignore = true)
    @Mapping(target = "retentionPeriod", ignore = true)
    ExpenseDraftApproval toExpenseEntity(ExpenseDraftApprovalCreateRequestDto dto);

    @Mapping(target = "drafter", ignore = true)
    @Mapping(target = "draftDepartment", ignore = true)
    @Mapping(target = "participants", ignore = true)
    @Mapping(target = "attachments", ignore = true)
    @Mapping(target = "steps", ignore = true)
    @Mapping(target = "retentionPeriod", ignore = true)
    OverseasTripApproval toOverseasEntity(OverseasTripApprovalCreateRequestDto dto);

    @Mapping(target = "drafter", ignore = true)
    @Mapping(target = "draftDepartment", ignore = true)
    @Mapping(target = "participants", ignore = true)
    @Mapping(target = "attachments", ignore = true)
    @Mapping(target = "steps", ignore = true)
    @Mapping(target = "retentionPeriod", ignore = true)
    WelfareExpenseApproval toWelfareEntity(WelfareExpenseApprovalCreateRequestDto dto);

    @Mapping(target = "drafter", ignore = true)
    @Mapping(target = "draftDepartment", ignore = true)
    @Mapping(target = "participants", ignore = true)
    @Mapping(target = "attachments", ignore = true)
    @Mapping(target = "steps", ignore = true)
    @Mapping(target = "retentionPeriod", ignore = true)
    BasicApproval toBasicEntity(BasicApprovalCreateRequestDto dto);

    @Mapping(target = "status", expression = "java(approval.getDisplayStatus(viewerId))")
    @Mapping(target = "drafterId", source = "approval.drafter.id")
    @Mapping(target = "drafterName", source = "approval.drafter.displayName")
    @Mapping(target = "draftDepartmentName", source = "approval.draftDepartment.name.description")
    @Mapping(target = "draftDate", source = "approval.createdAt")
    @Mapping(target = "approvalSteps", source = "approval.steps")
    @Mapping(target = "documentDetails", ignore = true)
    ApprovalDetailResponseDto toDetailResponseDto(
        Approval approval, @Context Long viewerId, @Context S3Service s3Service);

    @Mapping(target = "approverId", source = "approver.id")
    @Mapping(target = "approverName", source = "approver.displayName")
    @Mapping(target = "approverDepartmentName", source = "approver.department.name.description")
    ApprovalDetailResponseDto.ApprovalStepDetailDto toStepDetailDto(ApprovalStep step);

    @Mapping(target = "userId", source = "user.id")
    @Mapping(target = "userName", source = "user.displayName")
    @Mapping(target = "departmentName", source = "user.department.name.description")
    ApprovalDetailResponseDto.ApprovalParticipantDetailDto toParticipantDetailDto(
        ApprovalParticipant participant);

    @Mapping(target = "fileUrl", expression = "java(s3Service.getPresignedViewUrl(attachment.getS3Key()))")
    ApprovalDetailResponseDto.ApprovalAttachmentDetailDto toAttachmentDetailDto(
        ApprovalAttachment attachment, @Context S3Service s3Service);

    @Mapping(target = "status", expression = "java(approval.getDisplayStatus(viewerId))")
    @Mapping(target = "drafterName", source = "approval.drafter.displayName")
    @Mapping(target = "draftDate", source = "approval.createdAt")
    @Mapping(target = "approvalLine", ignore = true) // AfterMapping에서 처리
    @Mapping(target = "completedDate", ignore = true)
        // AfterMapping에서 처리
    ApprovalSummaryResponseDto toSummaryResponseDto(Approval approval, @Context Long viewerId);

    @AfterMapping
    default void afterToSummaryResponseDto(
        Approval approval,
        @MappingTarget ApprovalSummaryResponseDto.ApprovalSummaryResponseDtoBuilder builder) {
        // [작성자 > 승인자1 > 승인자2 ...] 형태로 결재라인 문자열 조합
        StringBuilder sb = new StringBuilder("[");
        sb.append(approval.getDrafter().getDisplayName());

        List<ApprovalStep> sortedSteps = approval.getSteps().stream()
            .sorted(Comparator.comparingInt(ApprovalStep::getSequence))
            .collect(Collectors.toList());

        for (ApprovalStep step : sortedSteps) {
            sb.append(" > ").append(step.getApprover().getDisplayName());
        }
        sb.append("]");
        builder.approvalLine(sb.toString());

        // 최종 승인 또는 반려인 경우 마지막 단계의 처리일을 완료일로 세팅
        if (approval.getStatus()
            == kr.co.awesomelead.groupware_backend.domain.approval.enums.ApprovalStatus.APPROVED
            || approval.getStatus()
            == kr.co.awesomelead.groupware_backend.domain.approval.enums.ApprovalStatus.REJECTED) {
            if (!sortedSteps.isEmpty()) {
                builder.completedDate(sortedSteps.get(sortedSteps.size() - 1).getProcessedAt());
            }
        }
    }
}
