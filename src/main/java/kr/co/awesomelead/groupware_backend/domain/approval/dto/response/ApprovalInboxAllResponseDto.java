package kr.co.awesomelead.groupware_backend.domain.approval.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import kr.co.awesomelead.groupware_backend.domain.approval.enums.ApprovalLineStatus;
import kr.co.awesomelead.groupware_backend.domain.approval.enums.ApprovalRouteRole;
import kr.co.awesomelead.groupware_backend.domain.approval.enums.ApprovalStatus;
import kr.co.awesomelead.groupware_backend.domain.approval.enums.ApprovalTargetType;
import kr.co.awesomelead.groupware_backend.domain.approval.enums.ApprovalType;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
@AllArgsConstructor
@Schema(description = "결재진행 전체 탭 응답")
public class ApprovalInboxAllResponseDto {

    @Schema(description = "전체 탭 문서 목록")
    private List<DocumentDto> documents;

    @Getter
    @Builder
    @AllArgsConstructor
    @Schema(description = "결재 문서 요약")
    public static class DocumentDto {
        @Schema(description = "문서번호", example = "기안및지출결의 경영지원부 20260108-30")
        private String documentNo;
        private Long documentId;
        private Long templateId;
        private String templateCode;
        private String templateName;
        private String title;
        private ApprovalType approvalType;
        private String approvalTypeLabel;
        private ApprovalStatus status;
        private String statusLabel;
        private Long drafterUserId;
        private String drafterUserName;
        @Schema(description = "기안자명(바로 사용용 alias)")
        private String drafterName;
        private Long drafterDepartmentId;
        private String drafterDepartmentName;
        private Boolean mine;
        @Schema(description = "기안일(상신일시)")
        private LocalDateTime draftedAt;
        private LocalDateTime submittedAt;
        private LocalDateTime completedAt;
        private LocalDateTime createdAt;
        private LocalDateTime modifiedAt;
        @Schema(description = "문서 전체 결재선(참조자/열람권자 제외)")
        private List<ApprovalLineDto> approvalLines;
        private List<MyLineDto> myLines;
    }

    @Getter
    @Builder
    @AllArgsConstructor
    @Schema(description = "문서 결재선 항목")
    public static class ApprovalLineDto {
        private Long lineId;
        private ApprovalRouteRole role;
        private String roleLabel;
        private ApprovalTargetType targetType;
        private Long targetUserId;
        private Long targetDepartmentId;
        private String targetName;
        private Integer sequenceNo;
        private ApprovalLineStatus lineStatus;
        private String lineStatusLabel;
    }

    @Getter
    @Builder
    @AllArgsConstructor
    @Schema(description = "현재 사용자 기준 관련 라인")
    public static class MyLineDto {
        private Long lineId;
        private ApprovalRouteRole role;
        private String roleLabel;
        private ApprovalTargetType targetType;
        private Long targetUserId;
        private Long targetDepartmentId;
        private String targetName;
        private Integer sequenceNo;
        private Boolean required;
        private ApprovalLineStatus lineStatus;
        private String lineStatusLabel;
    }
}
