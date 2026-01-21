package kr.co.awesomelead.groupware_backend.domain.visit.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
@Schema(description = "입퇴실 및 서명 기록 DTO")
public class VisitRecordResponseDto {

    @Schema(description = "입실 시간", example = "2026-02-01 10:00:00")
    private LocalDateTime entryTime;

    @Schema(description = "퇴실 시간", example = "2026-02-01 18:00:00")
    private LocalDateTime exitTime;

    @Schema(description = "서명 이미지 URL", example = "https://s3.bucket.com/signatures/abc.png")
    private String signatureUrl;
}
