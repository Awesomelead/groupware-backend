package kr.co.awesomelead.groupware_backend.domain.safetytraining.dto.request;

import com.fasterxml.jackson.annotation.JsonFormat;

import io.swagger.v3.oas.annotations.media.Schema;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import kr.co.awesomelead.groupware_backend.domain.department.enums.Company;
import kr.co.awesomelead.groupware_backend.domain.safetytraining.enums.SafetyEducationMethod;
import kr.co.awesomelead.groupware_backend.domain.safetytraining.enums.SafetyEducationType;

import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@NoArgsConstructor
@Schema(description = "안전보건 교육일지 수정 요청")
public class SafetyTrainingSessionUpdateRequestDto {

    @NotBlank(message = "제목은 필수입니다.")
    @Size(max = 200, message = "제목은 200자 이하여야 합니다.")
    @Schema(description = "교육 제목", example = "2026년 1분기 정기 안전보건교육(수정)")
    private String title;

    @NotNull(message = "교육 구분은 필수입니다.")
    @Schema(
            description =
                    "교육 구분 코드 (REGULAR: 정기교육, HIRING: 채용시, JOB_CHANGE: 작업내용 변경시, SPECIAL: 특별교육,"
                            + " MSDS: MSDS교육)",
            example = "REGULAR",
            allowableValues = {"REGULAR", "HIRING", "JOB_CHANGE", "SPECIAL", "MSDS"})
    private SafetyEducationType educationType;

    @NotEmpty(message = "교육 방법은 1개 이상 선택해야 합니다.")
    @Schema(
            description =
                    "교육 방법 코드 목록 (LECTURE: 강의, AUDIOVISUAL: 시청각, FIELD_TRAINING: 현장 교육,"
                            + " DEMONSTRATION: 시범 실습, TOUR: 견학, ROLE_PLAY: 역할연기)",
            example = "[\"LECTURE\", \"AUDIOVISUAL\"]",
            allowableValues = {
                "LECTURE",
                "AUDIOVISUAL",
                "FIELD_TRAINING",
                "DEMONSTRATION",
                "TOUR",
                "ROLE_PLAY"
            })
    private List<SafetyEducationMethod> educationMethods;

    @NotNull(message = "교육 시작 시간은 필수입니다.")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    @Schema(
            description = "교육 시작 시각 (ISO-8601, 초 단위, timezone 미포함)",
            example = "2026-03-24T08:30:00")
    private LocalDateTime startAt;

    @NotNull(message = "교육 종료 시간은 필수입니다.")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    @Schema(
            description = "교육 종료 시각 (ISO-8601, 초 단위, timezone 미포함)",
            example = "2026-03-24T10:30:00")
    private LocalDateTime endAt;

    @Schema(description = "교육 내용", example = "개인정보 보호 및 사내 보안 규정 안내")
    private String educationContent;

    @NotBlank(message = "교육 장소는 필수입니다.")
    @Size(max = 200, message = "교육 장소는 200자 이하여야 합니다.")
    @Schema(description = "교육 장소", example = "3층 대회의실")
    private String place;

    @NotNull(message = "교육 실시자 선택은 필수입니다.")
    @Schema(description = "교육 실시자 userId", example = "17")
    private Long instructorUserId;

    @NotNull(message = "회사 선택은 필수입니다.")
    @Schema(
            description = "회사 범위 코드 (AWESOME: 어썸리드, MARUI: 마루이)",
            example = "AWESOME",
            allowableValues = {"AWESOME", "MARUI"})
    private Company companyScope;
}
