package kr.co.awesomelead.groupware_backend.domain.annualleave.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "관리자용 연차 발송 목록 아이템")
public class AdminAnnualLeaveDispatchItemResponseDto {

    @Schema(description = "보낸 파일명", example = "2026년_연차현황.xlsx")
    private String originalFileName;

    @Schema(description = "시트명", example = "2026-06")
    private String sheetName;

    @Schema(description = "엑셀 파일 열람 URL(Presigned)", example = "https://...presigned-url")
    private String fileUrl;
}
