package kr.co.awesomelead.groupware_backend.domain.safetytraining.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@Schema(description = "안전보건 교육 세션 보고서 응답")
public class SafetyTrainingSessionReportResponseDto {

    @Schema(description = "세션 ID", example = "12")
    private Long sessionId;

    @Schema(description = "보고서 파일 URL", example = "https://...presigned-url")
    private String reportFileUrl;

    @Schema(description = "보고서 다운로드 파일명", example = "20260324_정기_안전보건교육.xlsx")
    private String reportFileName;
}
