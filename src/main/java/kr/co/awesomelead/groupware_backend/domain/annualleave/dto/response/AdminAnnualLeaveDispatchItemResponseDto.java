package kr.co.awesomelead.groupware_backend.domain.annualleave.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import kr.co.awesomelead.groupware_backend.domain.department.enums.Company;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@Schema(description = "관리자용 연차 발송 목록 아이템")
public class AdminAnnualLeaveDispatchItemResponseDto {

    @Schema(description = "발송 이력 ID", example = "10")
    private Long dispatchId;

    @Schema(description = "아이템 제목(월)", example = "7월")
    private String title;

    @Schema(description = "보낸 파일명", example = "2026년_연차현황.xlsx")
    private String originalFileName;

    @Schema(description = "시트명", example = "2026-06")
    private String sheetName;

    @Schema(description = "엑셀 파일 열람 URL(Presigned)", example = "https://...presigned-url")
    private String fileUrl;

    @Schema(description = "소속 회사", example = "AWESOME")
    private Company company;
}
