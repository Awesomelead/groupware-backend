package kr.co.awesomelead.groupware_backend.domain.visit.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import kr.co.awesomelead.groupware_backend.domain.visit.enums.VisitPurpose;
import kr.co.awesomelead.groupware_backend.domain.visit.enums.VisitType;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
@AllArgsConstructor
@Schema(description = "방문 접수 응답")
public class VisitResponseDto {

    @Schema(description = "방문 ID", example = "1")
    private Long id;

    @Schema(
            description = "방문 유형",
            example = "PRE_REGISTRATION",
            allowableValues = {"PRE_REGISTRATION", "ON_SITE"})
    private VisitType visitType;

    @Schema(description = "내방객 이름", example = "홍길동")
    private String visitorName;

    @Schema(description = "내방객 전화번호", example = "01012345678")
    private String visitorPhone;

    @Schema(description = "내방객 회사명", example = "어썸리드")
    private String visitorCompany;

    @Schema(description = "차량 번호", example = "12가3456")
    private String carNumber;

    @Schema(description = "방문 목적", example = "MEETING")
    private VisitPurpose purpose;

    @Schema(description = "방문 시작 일시", example = "2025-01-15T14:00:00")
    private LocalDateTime visitStartDate;

    @Schema(description = "방문 종료 일시", example = "2025-01-15T18:00:00")
    private LocalDateTime visitEndDate;

    @Schema(description = "담당자 ID", example = "1")
    private Long hostUserId;

    @Schema(description = "담당자 이름", example = "이담당")
    private String hostName;

    @Schema(description = "담당 부서", example = "개발팀")
    private String hostDepartment;

    @Schema(description = "방문 여부", example = "false")
    private boolean visited;

    @Schema(description = "신원 확인 여부", example = "false")
    private boolean verified;

    @Schema(description = "동행자 목록")
    private List<CompanionResponseDto> companions;

    @Getter
    @Builder
    @AllArgsConstructor
    @Schema(description = "동행자 정보")
    public static class CompanionResponseDto {

        @Schema(description = "동행자 이름", example = "김철수")
        private String name;

        @Schema(description = "동행자 전화번호", example = "01087654321")
        private String phoneNumber;
    }
}
