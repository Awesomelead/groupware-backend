package kr.co.awesomelead.groupware_backend.domain.payslip.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Schema(description = "직원용 급여명세서 상세 조회 응답")
public class EmployeePayslipDetailDto {

    @Schema(description = "급여명세서 ID", example = "1")
    private Long payslipId;

    @Schema(
            description = "파일 조회 URL",
            example = "https://bucket.s3.amazonaws.com/payslips/unique-file-key.pdf")
    private String presignedUrl;

    @Schema(description = "원본 파일명", example = "급여명세서(근로기준1)_10001_홍길동_202605.pdf")
    private String originalFileName;

    @Schema(description = "급여명세서 제목", example = "2026년 5월")
    private String payslipTitle;

    @Schema(description = "생성 일시", example = "2025-12-31T10:15:30")
    private LocalDateTime createdAt;
}
