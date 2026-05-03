package kr.co.awesomelead.groupware_backend.domain.approval.dto.response;

import kr.co.awesomelead.groupware_backend.domain.approval.enums.ApprovalStatus;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
@AllArgsConstructor
public class ApprovalSubmitResponseDto {

    private Long documentId;
    private ApprovalStatus status;
    private LocalDateTime submittedAt;
}
