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
        private Long id;
        private String name;
        private String position;
        private String departmentName;
    }

    @Getter
    @Builder
    @AllArgsConstructor
    @Schema(description = "열람권자 기본설정 항목")
    public static class DefaultViewerTargetDto {
        private Long id;

        @Schema(
                description = "타겟 타입",
                example = "USER",
                allowableValues = {"USER", "DEPARTMENT"})
        private ApprovalTargetType targetType;

        private Long targetUserId;
        private String targetUserName;
        private String targetUserPosition;
        private String targetUserDepartmentName;
        private Long targetDepartmentId;
        private String targetDepartmentName;
        private String targetName;
        private Integer sortOrder;
    }
}

