package kr.co.awesomelead.groupware_backend.domain.approval.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import kr.co.awesomelead.groupware_backend.domain.approval.enums.ApprovalTargetType;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.util.List;

@Getter
@Builder
@AllArgsConstructor
@Schema(description = "전자결재 개인설정 응답")
public class ApprovalPersonalSettingResponseDto {

    @Schema(description = "대결 사용 여부", example = "true")
    private Boolean delegateEnabled;

    @Schema(description = "대결자 정보")
    private DelegateUserDto delegateUser;

    @Schema(description = "대결 시작일", example = "2026-05-10")
    private LocalDate delegateStartDate;

    @Schema(description = "대결 종료일", example = "2026-05-20")
    private LocalDate delegateEndDate;

    @Schema(description = "서명 이미지 URL", example = "https://.../signature.png")
    private String signatureImageUrl;

    @Schema(description = "열람권자 기본설정 목록")
    private List<DefaultViewerTargetDto> defaultViewerTargets;

    @Getter
    @Builder
    @AllArgsConstructor
    @Schema(description = "대결자 정보")
    public static class DelegateUserDto {
        @Schema(description = "대결자 사용자 ID", example = "15")
        private Long id;

        @Schema(description = "대결자 이름", example = "더미사용자011")
        private String name;

        @Schema(description = "대결자 직급", example = "사원")
        private String position;

        @Schema(description = "대결자 부서명", example = "마루이 경비")
        private String departmentName;
    }

    @Getter
    @Builder
    @AllArgsConstructor
    @Schema(description = "열람권자 기본설정 항목")
    public static class DefaultViewerTargetDto {
        @Schema(description = "열람권자 기본설정 항목 ID", example = "201")
        private Long id;

        @Schema(
                description = "타겟 타입",
                example = "USER",
                allowableValues = {"USER", "DEPARTMENT"})
        private ApprovalTargetType targetType;

        @Schema(description = "사용자 타겟 ID(targetType=USER일 때)", example = "14")
        private Long targetUserId;

        @Schema(description = "사용자 타겟 이름", example = "고영민")
        private String targetUserName;

        @Schema(description = "사용자 타겟 직급", example = "사원")
        private String targetUserPosition;

        @Schema(description = "사용자 타겟 부서명", example = "경영지원부")
        private String targetUserDepartmentName;

        @Schema(description = "부서 타겟 ID(targetType=DEPARTMENT일 때)", example = "3")
        private Long targetDepartmentId;

        @Schema(description = "부서 타겟명", example = "경영지원부")
        private String targetDepartmentName;

        @Schema(description = "화면 표시용 타겟명", example = "[경영지원부] 고영민 (사원)")
        private String targetName;

        @Schema(description = "정렬순서", example = "1")
        private Integer sortOrder;
    }
}
