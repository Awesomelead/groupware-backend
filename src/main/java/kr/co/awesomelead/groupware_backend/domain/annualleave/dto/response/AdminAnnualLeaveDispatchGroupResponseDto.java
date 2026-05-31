package kr.co.awesomelead.groupware_backend.domain.annualleave.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "관리자용 연차 발송 목록 그룹 응답")
public class AdminAnnualLeaveDispatchGroupResponseDto {

    @Schema(description = "시트명", example = "2026-06")
    private String sheetName;

    @Schema(description = "그룹 제목(시트명)", example = "2026-06")
    private String title;

    @Schema(description = "해당 시트 발송 건수", example = "3")
    private int totalCount;

    @Schema(description = "해당 시트의 발송 목록")
    private List<AdminAnnualLeaveDispatchItemResponseDto> items;
}
