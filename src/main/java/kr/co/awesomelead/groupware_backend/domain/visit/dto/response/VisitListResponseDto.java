package kr.co.awesomelead.groupware_backend.domain.visit.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import kr.co.awesomelead.groupware_backend.domain.department.enums.Company;
import kr.co.awesomelead.groupware_backend.domain.visit.enums.VisitCategory;
import kr.co.awesomelead.groupware_backend.domain.visit.enums.VisitStatus;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@Getter
@Builder
@AllArgsConstructor
@Schema(description = "직원용 방문 목록 응답 DTO")
public class VisitListResponseDto {

    @Schema(description = "방문 ID", example = "1")
    private Long id;

    @Schema(description = "내방객 소속 회사명", example = "어썸테크")
    private String visitorCompany;

    @Schema(description = "방문 회사", example = "어썸리드")
    private Company hostCompany;

    @Schema(description = "내방객 이름", example = "홍길동")
    private String visitorName;

    @Schema(description = "담당자 목록")
    private List<VisitHostResponseDto> hosts;

    @Schema(description = "방문 시작일", example = "2026-02-01")
    private LocalDate startDate;

    @Schema(description = "방문 종료일", example = "2026-02-05")
    private LocalDate endDate;

    @Schema(description = "희망 입실 시간", example = "14:00:00")
    private LocalTime plannedEntryTime;

    @Schema(description = "희망 퇴실 시간", example = "18:00:00")
    private LocalTime plannedExitTime;

    @Schema(description = "실제 입실 시간", example = "2026-02-01T14:10:00")
    private LocalDateTime entryTime;

    @Schema(description = "실제 퇴실 시간", example = "2026-02-01T17:55:00")
    private LocalDateTime exitTime;

    @Schema(description = "방문 상태", example = "방문 중")
    private VisitStatus status;

    @Schema(description = "방문 유형", example = "PRE_ONE_DAY")
    private VisitCategory visitCategory;
}
