package kr.co.awesomelead.groupware_backend.domain.visit.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "직원용 사전 예약자 입실 등록 및 시간 수정 요청")
public class AdminCheckInRequestDto {

    @NotNull(message = "입실 시간은 필수입니다.")
    @PastOrPresent(message = "입실 시간은 미래일 수 없습니다.")
    @Schema(description = "실제 입실 시간 (예약 기간 내)", example = "2026-11-04T10:00:00")
    private LocalDateTime checkInTime;
}
