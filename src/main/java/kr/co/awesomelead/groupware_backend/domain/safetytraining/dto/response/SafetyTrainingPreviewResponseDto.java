package kr.co.awesomelead.groupware_backend.domain.safetytraining.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@Schema(description = "안전보건 교육 엑셀 파일 미리보기 응답")
public class SafetyTrainingPreviewResponseDto {

    @Schema(description = "미리보기 파일 URL", example = "https://...presigned-url")
    private String previewFileUrl;

    @Schema(description = "미리보기 다운로드 파일명", example = "20260324_정기_안전보건교육.xlsx")
    private String previewFileName;

    @Schema(description = "교육 대상 인원 수", example = "49")
    private int targetCount;

    @Schema(description = "교육 참석 인원 수(미리보기는 0)", example = "0")
    private int attendedCount;

    @Schema(description = "교육 미참석 인원 수(미리보기는 0)", example = "0")
    private int absentCount;
}
