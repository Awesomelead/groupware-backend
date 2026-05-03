package kr.co.awesomelead.groupware_backend.domain.approval.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import kr.co.awesomelead.groupware_backend.domain.approval.enums.ApprovalType;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class ApprovalDraftUpsertRequestDto {

    private Long documentId;

    @NotNull private Long templateId;

    private String title;

    private String contentDelta;

    private String contentHtml;

    private ApprovalType approvalType;

    private Long receiverDepartmentId;

    @Valid private List<ApprovalLineRequestDto> lines;
}
