package kr.co.awesomelead.groupware_backend.domain.annualleave.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
@Schema(description = "관리자용 연차 발송 목록 그룹 응답")
public class AdminAnnualLeaveDispatchGroupResponseDto {

    @Schema(description = "그룹 기준 연도", example = "2026년")
    private String sheetName;

    @Schema(description = "그룹 제목(연도)", example = "2026년")
    private String title;

    @Schema(description = "해당 연도 발송 건수", example = "3")
    private int totalCount;

    @Schema(description = "해당 연도의 발송 목록")
    private List<AdminAnnualLeaveDispatchItemResponseDto> items;
}
