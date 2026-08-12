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
@Schema(description = "전자결재 문서 회수 응답")
public class ApprovalRecallResponseDto {

    @Schema(description = "문서 ID", example = "101")
    private Long documentId;

    @Schema(description = "문서번호", example = "기안및지출결의 경영지원부 20260108-30")
    private String documentNo;

    @Schema(description = "문서 상태", example = "RECALLED")
    private ApprovalStatus status;

    @Schema(description = "문서 상태 한글 라벨", example = "회수")
    private String statusLabel;

    @Schema(description = "제목", example = "국외출장여비정산서")
    private String title;

    @Schema(description = "회수일시")
    private LocalDateTime recalledAt;
}
