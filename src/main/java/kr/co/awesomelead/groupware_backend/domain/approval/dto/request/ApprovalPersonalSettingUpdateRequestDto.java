package kr.co.awesomelead.groupware_backend.domain.approval.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;

import jakarta.validation.Valid;

import kr.co.awesomelead.groupware_backend.domain.approval.enums.ApprovalTargetType;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.util.List;

@Getter
@Setter
@Schema(description = "전자결재 개인설정 저장 요청")
public class ApprovalPersonalSettingUpdateRequestDto {

    @Schema(description = "대결 사용 여부", example = "true", defaultValue = "false")
    private Boolean delegateEnabled;

    @Schema(description = "대결자 사용자 ID(delegateEnabled=true일 때 필수)", example = "15")
    private Long delegateUserId;

    @Schema(description = "대결 시작일(delegateEnabled=true일 때 필수)", example = "2026-05-10")
    private LocalDate delegateStartDate;

    @Schema(description = "대결 종료일(delegateEnabled=true일 때 필수)", example = "2026-05-20")
    private LocalDate delegateEndDate;

    @Valid
    @Schema(description = "열람권자 기본설정 목록(사용자/부서 혼합 가능)")
    private List<DefaultViewerTargetRequestDto> defaultViewerTargets;

    @Getter
    @Setter
    @Schema(description = "열람권자 기본설정 항목")
    public static class DefaultViewerTargetRequestDto {

        @Schema(
                description = "타겟 타입 (USER면 targetUserId 필수, DEPARTMENT면 targetDepartmentId 필수)",
                example = "USER",
                allowableValues = {"USER", "DEPARTMENT"})
        private ApprovalTargetType targetType;

        @Schema(description = "타겟 사용자 ID(targetType=USER일 때 필수)", example = "14")
        private Long targetUserId;

        @Schema(description = "타겟 부서 ID(targetType=DEPARTMENT일 때 필수)", example = "3")
        private Long targetDepartmentId;
    }
}
