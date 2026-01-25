package kr.co.awesomelead.groupware_backend.domain.visit.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import kr.co.awesomelead.groupware_backend.domain.department.enums.DepartmentName;
import kr.co.awesomelead.groupware_backend.domain.visit.enums.VisitPurpose;
import kr.co.awesomelead.groupware_backend.domain.visit.enums.VisitStatus;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
@AllArgsConstructor
@Schema(description = "내 방문 상세 조회 응답 (내방객용)")
public class MyVisitDetailResponseDto {

    @Schema(description = "방문 ID", example = "1")
    private Long visitId;

    @Schema(description = "내방객 회사명", example = "어썸테크")
    private String visitorCompany;

    @Schema(description = "내방객 이름", example = "홍길동")
    private String visitorName;

    @Schema(description = "내방객 전화번호", example = "01012345678")
    private String visitorPhoneNumber;

    @Schema(description = "방문 목적", example = "고객 검수")
    private VisitPurpose purpose;

    @Schema(description = "담당 부서명", example = "환경안전부")
    private DepartmentName departmentName;

    @Schema(description = "방문 상태", example = "방문 전")
    private VisitStatus status;

    @Schema(description = "방문 시작일", example = "2024-07-01")
    private LocalDate startDate;

    @Schema(description = "방문 종료일", example = "2024-07-01")
    private LocalDate endDate;

    @Schema(description = "입실 시간", example = "2024-07-01T14:30:00")
    private LocalDateTime entryTime;

    @Schema(description = "퇴실 시간", example = "2024-07-01T16:30:00")
    private LocalDateTime exitTime;

    @Schema(description = "입퇴실 및 서명 기록 리스트")
    private List<VisitRecordResponseDto> records;
}
