package kr.co.awesomelead.groupware_backend.domain.annualleave.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "관리자용 연차 발송 상세 응답")
public class AdminAnnualLeaveDispatchDetailResponseDto {

    @Schema(description = "발송 이력 ID", example = "10")
    private Long dispatchId;

    @Schema(description = "보낸 파일명", example = "2026_연차현황.xlsx")
    private String originalFileName;

    @Schema(description = "시트명", example = "2026-06")
    private String sheetName;

    @Schema(description = "업로드 시각", example = "2026-05-31T15:29:04")
    private LocalDateTime createdAt;

    @Schema(description = "엑셀 파일 열람 URL(Presigned)", example = "https://...presigned-url")
    private String fileUrl;
}
