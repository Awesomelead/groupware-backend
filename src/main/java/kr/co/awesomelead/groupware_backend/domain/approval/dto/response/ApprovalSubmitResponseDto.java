package kr.co.awesomelead.groupware_backend.domain.approval.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import kr.co.awesomelead.groupware_backend.domain.approval.enums.ApprovalRouteRole;
import kr.co.awesomelead.groupware_backend.domain.approval.enums.ApprovalStatus;
import kr.co.awesomelead.groupware_backend.domain.approval.enums.ApprovalTargetType;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
@AllArgsConstructor
@Schema(description = "전자결재 상신 응답")
public class ApprovalSubmitResponseDto {

    @Schema(description = "문서 ID", example = "101")
    private Long documentId;

    @Schema(description = "문서번호", example = "기안및지출결의 경영지원부 20260108-30")
    private String documentNo;

    @Schema(description = "문서 상태")
    private ApprovalStatus status;

    @Schema(description = "기안자 ID", example = "14")
    private Long drafterUserId;

    @Schema(description = "기안자명", example = "고영민")
    private String drafterUserName;

    @Schema(description = "제목", example = "국외출장여비정산서")
    private String title;

    @Schema(description = "결재선(참조자/열람권자 제외)")
    private List<ApprovalLineDto> approvalLines;

    @Schema(description = "기안일(상신일시)")
    private LocalDateTime draftedAt;

    @Schema(description = "상신일시(기안일과 동일)")
    private LocalDateTime submittedAt;

    @Schema(description = "완료일시(완결 시 세팅)")
    private LocalDateTime completedAt;

    @Getter
    @Builder
    @AllArgsConstructor
    @Schema(description = "결재선 항목")
    public static class ApprovalLineDto {
        private Long lineId;
        private ApprovalRouteRole role;
        private String roleLabel;
        private ApprovalTargetType targetType;
        private Long targetUserId;
        private Long targetDepartmentId;
        private String targetName;
        private Integer sequenceNo;
    }
}
