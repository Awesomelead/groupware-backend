package kr.co.awesomelead.groupware_backend.domain.approval.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import kr.co.awesomelead.groupware_backend.domain.approval.enums.ApprovalRouteRole;
import kr.co.awesomelead.groupware_backend.domain.approval.enums.ApprovalSavedLineType;
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
@Schema(description = "저장 결재선 응답")
public class SavedApprovalLineResponseDto {

    @Schema(description = "저장 결재선 ID", example = "101")
    private Long id;

    @Schema(
            description = "결재선 타입",
            example = "PERSONAL",
            allowableValues = {"PERSONAL", "DEPARTMENT"})
    private ApprovalSavedLineType lineType;

    @Schema(description = "결재선 타입 한글 라벨", example = "개인 결재선")
    private String lineTypeLabel;
    @Schema(description = "결재선명", example = "환경안전부 기본 결재선")
    private String lineName;

    @Schema(
            description = "결재유형",
            example = "INTERNAL",
            allowableValues = {"INTERNAL", "COOPERATIVE"})
    private ApprovalType approvalType;

    @Schema(description = "결재유형 한글 라벨", example = "내부결재")
    private String approvalTypeLabel;

    @Schema(description = "기본결재선 여부", example = "true")
    private Boolean isDefault;
    @Schema(description = "소유자 사용자 ID(lineType=PERSONAL에서 사용)", example = "14")
    private Long ownerUserId;
    @Schema(description = "소유자 이름(lineType=PERSONAL에서 사용)", example = "고영민")
    private String ownerUserName;
    @Schema(description = "부서 ID(lineType=DEPARTMENT에서 사용)", example = "3")
    private Long departmentId;
    @Schema(description = "부서명(lineType=DEPARTMENT에서 사용)", example = "환경안전부")
    private String departmentName;
    @Schema(description = "생성자 사용자 ID", example = "14")
    private Long createdByUserId;
    @Schema(description = "생성자 이름", example = "고영민")
    private String createdByUserName;
    @Schema(description = "생성일시")
    private LocalDateTime createdAt;
    @Schema(description = "최종 수정일시")
    private LocalDateTime modifiedAt;
    @Schema(description = "결재선 상세 라인 목록")
    private List<LineDetailDto> lines;
    @Schema(description = "결재칸 미리보기 데이터")
    private ApprovalBoxPreviewDto approvalBoxPreview;

    @Getter
    @Builder
    @AllArgsConstructor
    @Schema(description = "결재칸 미리보기")
    public static class ApprovalBoxPreviewDto {
        @Schema(description = "결재칸에 표시될 결재선 목록 (sequenceNo 오름차순)")
        private List<ApprovalBoxSlotDto> slots;
    }

    @Getter
    @Builder
    @AllArgsConstructor
    @Schema(description = "결재칸 단일 슬롯")
    public static class ApprovalBoxSlotDto {
        @Schema(description = "결재 순서", example = "1")
        private Integer sequenceNo;

        @Schema(
                description = "타겟 타입",
                example = "USER",
                allowableValues = {"USER", "DEPARTMENT"})
        private ApprovalTargetType targetType;

        @Schema(description = "타겟 사용자 ID(targetType=USER일 때)", example = "14")
        private Long targetUserId;
        @Schema(description = "타겟 사용자 이름", example = "고영민")
        private String targetUserName;
        @Schema(description = "타겟 사용자 직급", example = "사원")
        private String targetUserPosition;
        @Schema(description = "타겟 사용자 부서명", example = "환경안전부")
        private String targetUserDepartmentName;
        @Schema(description = "타겟 부서 ID(targetType=DEPARTMENT일 때)", example = "3")
        private Long targetDepartmentId;
        @Schema(description = "타겟 부서명", example = "환경안전부")
        private String targetDepartmentName;
        @Schema(description = "표시용 타겟명", example = "[환경안전부] 고은서 (사원)")
        private String targetName;
    }

    @Getter
    @Builder
    @AllArgsConstructor
    @Schema(description = "저장 결재선 상세 항목")
    public static class LineDetailDto {
        @Schema(description = "결재선 상세 항목 ID", example = "1001")
        private Long id;

        @Schema(
                description = "결재선 역할",
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

        @Schema(
                description = "타겟 타입",
                example = "USER",
                allowableValues = {"USER", "DEPARTMENT"})
        private ApprovalTargetType targetType;

        @Schema(description = "타겟 사용자 ID(targetType=USER일 때)", example = "14")
        private Long targetUserId;
        @Schema(description = "타겟 사용자명", example = "고영민")
        private String targetUserName;
        @Schema(description = "타겟 사용자 직급", example = "사원")
        private String targetUserPosition;
        @Schema(description = "타겟 사용자 부서명", example = "경영지원부")
        private String targetUserDepartmentName;
        @Schema(description = "타겟 부서 ID(targetType=DEPARTMENT일 때)", example = "3")
        private Long targetDepartmentId;
        @Schema(description = "타겟 부서명", example = "환경안전부")
        private String targetDepartmentName;
        @Schema(description = "표시용 타겟명", example = "[경영지원부] 고영민 (사원)")
        private String targetName;
        @Schema(description = "정렬 순서", example = "1")
        private Integer sequenceNo;
        @Schema(description = "필수 여부", example = "true")
        private Boolean required;
    }
}
