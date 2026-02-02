package kr.co.awesomelead.groupware_backend.domain.visit.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;

import jakarta.validation.constraints.NotNull;

import kr.co.awesomelead.groupware_backend.domain.visit.enums.VisitStatus;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "방문 신청 처리 요청")
public class VisitProcessRequestDto {

    @NotNull(message = "처리 상태(APPROVED/REJECTED)는 필수입니다.")
    @Schema(description = "처리 상태", example = "반려")
    private VisitStatus status; // Enum: APPROVED, REJECTED

    @Schema(description = "반려 사유 (반려 시 필수)", example = "방문 목적이 불분명합니다.")
    private String rejectionReason;
}
