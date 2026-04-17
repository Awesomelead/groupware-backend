package kr.co.awesomelead.groupware_backend.domain.education.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "교육 보고서 서명 현황 응답 DTO")
public class EduReportSignatureStatusDto {

    @Schema(description = "직원 이름", example = "홍길동")
    private String displayName;

    @Schema(description = "부서명", example = "경영지원부")
    private String departmentName;

    @Schema(description = "직급", example = "사원")
    private String position;

    @Schema(
            description = "서명 이미지 Presigned URL (서명 없으면 null)",
            example = "https://s3.amazonaws.com/...")
    private String signatureUrl;

    @Schema(description = "서명 여부", example = "true")
    private boolean isSigned;
}
