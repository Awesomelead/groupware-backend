package kr.co.awesomelead.groupware_backend.domain.approval.dto.request;

import jakarta.validation.constraints.NotNull;

import kr.co.awesomelead.groupware_backend.domain.approval.enums.ApprovalRouteRole;
import kr.co.awesomelead.groupware_backend.domain.approval.enums.ApprovalTargetType;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ApprovalTemplateLineUpsertRequestDto {

    @NotNull private ApprovalRouteRole role;

    @NotNull private ApprovalTargetType targetType;

    private Long targetUserId;

    private Long targetDepartmentId;

    private Integer sequenceNo;

    @NotNull private Boolean required;
}
