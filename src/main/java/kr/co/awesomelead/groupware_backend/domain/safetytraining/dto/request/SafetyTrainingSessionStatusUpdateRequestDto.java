package kr.co.awesomelead.groupware_backend.domain.safetytraining.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import kr.co.awesomelead.groupware_backend.domain.safetytraining.enums.SafetyTrainingSessionStatus;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@Schema(description = "안전보건 교육 세션 상태 변경 요청")
public class SafetyTrainingSessionStatusUpdateRequestDto {

    @NotNull(message = "세션 상태는 필수입니다.")
    @Schema(
            description = "세션 상태 코드 (OPEN: 진행중, CLOSED: 정상 마감)",
            example = "CLOSED",
            allowableValues = {"OPEN", "CLOSED"})
    private SafetyTrainingSessionStatus status;

    @Size(max = 2000, message = "교육 미참석 사유는 2000자 이하여야 합니다.")
    @Schema(
            description =
                    "교육 미참석 사유(세션 단위). status=CLOSED + 결석자 존재 시 필수, "
                            + "status=OPEN 전환 시에는 null로 초기화됩니다.",
            example = "현장 장비 점검으로 일부 인원 교육 참여 불가")
    private String absentReasonSummary;
}
