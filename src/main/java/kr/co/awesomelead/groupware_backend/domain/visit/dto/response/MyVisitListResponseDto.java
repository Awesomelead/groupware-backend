package kr.co.awesomelead.groupware_backend.domain.visit.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import kr.co.awesomelead.groupware_backend.domain.visit.enums.VisitCategory;
import kr.co.awesomelead.groupware_backend.domain.visit.enums.VisitPurpose;
import kr.co.awesomelead.groupware_backend.domain.visit.enums.VisitStatus;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.util.List;

@Getter
@Builder
@AllArgsConstructor
@Schema(description = "내 방문 목록 조회 응답 (내방객용)")
public class MyVisitListResponseDto {

    @Schema(description = "방문 ID", example = "1")
    private Long visitId;

    @Schema(description = "내방객 이름", example = "홍길동")
    private String visitorName;

    @Schema(description = "방문 상태", example = "방문 전")
    private VisitStatus status;

    @Schema(description = "방문 유형", example = "PRE_ONE_DAY")
    private VisitCategory visitCategory;

    @Schema(description = "내방객 회사명", example = "어썸리드")
    private String visitorCompany;

    @Schema(description = "방문 목적", example = "MEETING")
    private VisitPurpose purpose;

    @Schema(description = "방문 시작일", example = "2024-07-01")
    private LocalDate startDate;

    @Schema(description = "방문 종료일", example = "2024-07-01")
    private LocalDate endDate;

    @Schema(description = "담당자 목록")
    private List<VisitHostResponseDto> hosts;
}
