package kr.co.awesomelead.groupware_backend.domain.visit.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@AllArgsConstructor
@Schema(description = "입퇴실 및 서명 기록 DTO")
public class VisitRecordResponseDto {

    @Schema(
            description = "방문 기록 ID (PK). 입실 시간 수정 API의 recordId 및 퇴실 API의 visitRecordId로 사용",
            example = "10")
    private Long id;

    @Schema(description = "입실 시간", example = "2026-02-01 10:00:00")
    private LocalDateTime entryTime;

    @Schema(description = "퇴실 시간", example = "2026-02-01 18:00:00")
    private LocalDateTime exitTime;

    @Schema(description = "퇴실 시간 마지막 등록/수정자")
    private VisitRecordExitTimeUpdatedByResponseDto exitTimeUpdatedBy;

    @Schema(description = "서명 이미지 URL", example = "https://s3.bucket.com/signatures/abc.png")
    private String signatureUrl;
}
