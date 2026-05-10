package kr.co.awesomelead.groupware_backend.domain.approval.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import kr.co.awesomelead.groupware_backend.domain.approval.enums.ApprovalStatus;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
@AllArgsConstructor
@Schema(description = "전자결재 임시저장 응답")
public class ApprovalDraftResponseDto {

    @Schema(description = "임시저장 문서 ID", example = "101")
    private Long documentId;

    @Schema(
            description = "문서 상태",
            example = "DRAFT",
            allowableValues = {
                "DRAFT",
                "IN_PROGRESS",
                "APPROVED",
                "REJECTED",
                "RECALLED",
                "CANCELED"
            })
    private ApprovalStatus status;

    @Schema(description = "최종 수정일시", example = "2026-05-11T01:20:30")
    private LocalDateTime updatedAt;
}
