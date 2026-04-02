package kr.co.awesomelead.groupware_backend.domain.education.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;

import jakarta.validation.constraints.NotNull;

import kr.co.awesomelead.groupware_backend.domain.education.enums.EduReportStatus;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@Schema(description = "부서교육 상태 변경 요청")
public class EduReportStatusUpdateRequestDto {

    @NotNull(message = "상태는 필수입니다.")
    @Schema(
            description = "부서교육 상태 코드 (OPEN: 진행중, CLOSED: 마감)",
            example = "CLOSED",
            allowableValues = {"OPEN", "CLOSED"})
    private EduReportStatus status;
}
