package kr.co.awesomelead.groupware_backend.domain.safetytraining.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;

import kr.co.awesomelead.groupware_backend.domain.department.enums.Company;
import kr.co.awesomelead.groupware_backend.domain.safetytraining.enums.SafetyEducationType;
import kr.co.awesomelead.groupware_backend.domain.safetytraining.enums.SafetyTrainingSessionStatus;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@Schema(description = "안전보건 교육 세션 목록 조회 필터")
public class SafetyTrainingSessionSearchConditionDto {

    @Schema(
            description = "회사 필터 (AWESOME: 어썸리드, MARUI: 마루이)",
            example = "AWESOME",
            allowableValues = {"AWESOME", "MARUI"})
    private Company companyScope;

    @Schema(
            description =
                    "교육 구분 필터 (REGULAR: 정기교육, HIRING: 채용시, JOB_CHANGE: 작업내용 변경시,"
                            + " SPECIAL: 특별교육, MSDS: MSDS교육)",
            example = "SPECIAL",
            allowableValues = {"REGULAR", "HIRING", "JOB_CHANGE", "SPECIAL", "MSDS"})
    private SafetyEducationType educationType;

    @Schema(
            description = "세션 상태 필터 (OPEN: 진행중, CLOSED: 마감)",
            example = "OPEN",
            allowableValues = {"OPEN", "CLOSED"})
    private SafetyTrainingSessionStatus status;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    @Schema(description = "교육 시작시각 하한(이 값 이상)", example = "2026-03-01T00:00:00")
    private LocalDateTime startAtFrom;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    @Schema(description = "교육 시작시각 상한(이 값 이하)", example = "2026-03-31T23:59:59")
    private LocalDateTime startAtTo;
}
