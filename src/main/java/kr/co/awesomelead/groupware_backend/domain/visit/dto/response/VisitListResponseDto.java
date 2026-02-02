package kr.co.awesomelead.groupware_backend.domain.visit.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import kr.co.awesomelead.groupware_backend.domain.department.enums.DepartmentName;
import kr.co.awesomelead.groupware_backend.domain.visit.enums.VisitStatus;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;

@Getter
@Builder
@AllArgsConstructor
@Schema(description = "직원용 방문 목록 응답 DTO")
public class VisitListResponseDto {

    @Schema(description = "방문 ID", example = "1")
    private Long id;

    @Schema(description = "내방객 소속 회사명", example = "어썸테크")
    private String visitorCompany;

    @Schema(description = "내방객 이름", example = "홍길동")
    private String visitorName;

    @Schema(description = "담당 부서", example = "경영지원부")
    private DepartmentName hostDepartmentName;

    @Schema(description = "방문 시작일", example = "2026-02-01")
    private LocalDate startDate;

    @Schema(description = "방문 종료일", example = "2026-02-05")
    private LocalDate endDate;

    @Schema(description = "방문 상태", example = "방문 중")
    private VisitStatus status;
}
