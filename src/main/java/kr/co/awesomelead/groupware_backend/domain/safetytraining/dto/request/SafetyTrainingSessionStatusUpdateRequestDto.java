package kr.co.awesomelead.groupware_backend.domain.safetytraining.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;

import jakarta.validation.constraints.NotNull;

import kr.co.awesomelead.groupware_backend.domain.safetytraining.enums.SafetyTrainingSessionStatus;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@Schema(description = "안전보건 교육 세션 상태 변경 요청")
public class SafetyTrainingSessionStatusUpdateRequestDto {

    @NotNull(message = "세션 상태는 필수입니다.")
    @Schema(
            description = "세션 상태 코드 (OPEN: 진행중, CLOSED: 마감)",
            example = "CLOSED",
            allowableValues = {"OPEN", "CLOSED"})
    private SafetyTrainingSessionStatus status;
}
