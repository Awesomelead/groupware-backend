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

    @Schema(description = "현재 사용자 소속 부서 ID(부서결재함 등 부서 기준 화면에서 사용)", example = "3")
    private Long myDepartmentId;

    @Schema(description = "현재 사용자 소속 부서명(부서결재함 등 부서 기준 화면에서 사용)", example = "환경안전부")
    private String myDepartmentName;

    @Schema(description = "전체 탭 문서 목록")
    private List<DocumentDto> documents;

    @Getter
    @Builder
    @AllArgsConstructor
    @Schema(description = "결재 문서 요약")
    public static class DocumentDto {
        @Schema(description = "문서번호", example = "기안및지출결의 경영지원부 20260108-30")
        private String documentNo;

        @Schema(description = "문서 ID", example = "101")
        private Long documentId;

        @Schema(description = "양식 ID", example = "1")
        private Long templateId;

        @Schema(description = "양식 코드", example = "EXPENSE_DRAFT")
        private String templateCode;

        @Schema(description = "양식명", example = "기안및지출결의")
        private String templateName;

        @Schema(description = "문서 제목", example = "1분기 출장비 정산")
        private String title;

        @Schema(
                description = "결재유형",
                example = "INTERNAL",
                allowableValues = {"INTERNAL", "COOPERATIVE"})
        private ApprovalType approvalType;

        @Schema(description = "결재유형 한글 라벨", example = "내부결재")
        private String approvalTypeLabel;

        @Schema(
                description = "문서 상태",
                example = "IN_PROGRESS",
                allowableValues = {
                    "DRAFT",
                    "IN_PROGRESS",
                    "APPROVED",
                    "REJECTED",
                    "RECALLED",
                    "CANCELED"
                })
        private ApprovalStatus status;

        @Schema(description = "문서 상태 한글 라벨", example = "결재진행")
        private String statusLabel;

        @Schema(description = "기안자 사용자 ID", example = "14")
        private Long drafterUserId;

        @Schema(description = "기안자 이름", example = "고영민")
        private String drafterUserName;

        @Schema(description = "기안자명(바로 사용용 alias)")
        private String drafterName;

        @Schema(description = "기안자 부서 ID", example = "3")
        private Long drafterDepartmentId;

        @Schema(description = "기안자 부서명", example = "경영지원부")
        private String drafterDepartmentName;

        @Schema(description = "내 문서 여부(기안자 본인 여부)", example = "true")
        private Boolean mine;

        @Schema(description = "기안일(상신일시)")
        private LocalDateTime draftedAt;

        @Schema(description = "상신일시")
        private LocalDateTime submittedAt;

        @Schema(description = "완료일시(완결 시)")
        private LocalDateTime completedAt;

        @Schema(description = "최초 생성일시")
        private LocalDateTime createdAt;

        @Schema(description = "최종 수정일시")
        private LocalDateTime modifiedAt;

        @Schema(description = "문서 전체 결재선(참조자/열람권자 제외)")
        private List<ApprovalLineDto> approvalLines;

        @Schema(description = "현재 사용자 기준 관련 라인(내 결재선/내 부서 라인)")
        private List<MyLineDto> myLines;
    }

    @Getter
    @Builder
    @AllArgsConstructor
    @Schema(description = "문서 결재선 항목")
    public static class ApprovalLineDto {
        @Schema(description = "결재 라인 ID", example = "1001")
        private Long lineId;

        @Schema(
                description = "결재 라인 역할",
                example = "APPROVAL_LINE",
                allowableValues = {
                    "APPROVAL_LINE",
                    "AGREEMENT_REQUIRED",
                    "AGREEMENT_OPTIONAL",
                    "REFERENCE",
                    "VIEWER",
                    "RECEIVER_DEPARTMENT"
                })
        private ApprovalRouteRole role;

        @Schema(description = "결재 라인 역할 한글 라벨", example = "결재선")
        private String roleLabel;

        @Schema(
                description = "결재 라인 타겟 타입",
                example = "USER",
                allowableValues = {"USER", "DEPARTMENT"})
        private ApprovalTargetType targetType;

        @Schema(description = "타겟 사용자 ID(targetType=USER일 때)", example = "14")
        private Long targetUserId;

        @Schema(description = "타겟 부서 ID(targetType=DEPARTMENT일 때)", example = "3")
        private Long targetDepartmentId;

        @Schema(description = "타겟 표시명", example = "[경영지원부] 고영민 (사원)")
        private String targetName;

        @Schema(description = "결재 순서", example = "1")
        private Integer sequenceNo;

        @Schema(
                description = "결재 라인 상태",
                example = "PENDING",
                allowableValues = {
                    "WAITING",
                    "PENDING",
                    "APPROVED",
                    "REJECTED",
                    "SKIPPED",
                    "CANCELED"
                })
        private ApprovalLineStatus lineStatus;

        @Schema(description = "결재 라인 상태 한글 라벨", example = "결재대기")
        private String lineStatusLabel;
    }

    @Getter
    @Builder
    @AllArgsConstructor
    @Schema(description = "현재 사용자 기준 관련 라인")
    public static class MyLineDto {
        @Schema(description = "결재 라인 ID", example = "1001")
        private Long lineId;

        @Schema(
                description = "결재 라인 역할",
                example = "APPROVAL_LINE",
                allowableValues = {
                    "APPROVAL_LINE",
                    "AGREEMENT_REQUIRED",
                    "AGREEMENT_OPTIONAL",
                    "REFERENCE",
                    "VIEWER",
                    "RECEIVER_DEPARTMENT"
                })
        private ApprovalRouteRole role;

        @Schema(description = "결재 라인 역할 한글 라벨", example = "결재선")
        private String roleLabel;

        @Schema(
                description = "결재 라인 타겟 타입",
                example = "USER",
                allowableValues = {"USER", "DEPARTMENT"})
        private ApprovalTargetType targetType;

        @Schema(description = "타겟 사용자 ID(targetType=USER일 때)", example = "14")
        private Long targetUserId;

        @Schema(description = "타겟 부서 ID(targetType=DEPARTMENT일 때)", example = "3")
        private Long targetDepartmentId;

        @Schema(description = "타겟 표시명", example = "[경영지원부] 고영민 (사원)")
        private String targetName;

        @Schema(description = "결재 순서", example = "1")
        private Integer sequenceNo;

        @Schema(description = "필수 여부", example = "true")
        private Boolean required;

        @Schema(
                description = "결재 라인 상태",
                example = "PENDING",
                allowableValues = {
                    "WAITING",
                    "PENDING",
                    "APPROVED",
                    "REJECTED",
                    "SKIPPED",
                    "CANCELED"
                })
        private ApprovalLineStatus lineStatus;

        @Schema(description = "결재 라인 상태 한글 라벨", example = "결재대기")
        private String lineStatusLabel;
    }
}
