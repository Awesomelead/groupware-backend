package kr.co.awesomelead.groupware_backend.domain.visit.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "퇴실 처리 요청")
public class CheckOutRequestDto {

    @Schema(description = "방문 ID", example = "1", required = true)
    private Long visitId;

    @Schema(description = "퇴실 시간", example = "2025-01-15T18:00:00", required = true)
    private LocalDateTime checkOutTime;
}
