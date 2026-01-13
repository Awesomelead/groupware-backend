package kr.co.awesomelead.groupware_backend.domain.payslip.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import kr.co.awesomelead.groupware_backend.domain.payslip.enums.PayslipStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Schema(description = "직원용 급여명세서 목록 조회 응답")
public class EmployeePayslipSummaryDto {

    @Schema(description = "급여명세서 ID", example = "1")
    private Long payslipId;
    @Schema(description = "급여명세서 상태", example = "PENDING")
    private PayslipStatus status;
    @Schema(description = "원본 파일명", example = "홍길동_20251231_급여명세서.pdf")
    private String originalFileName;
    @Schema(description = "생성 일시", example = "2025-12-31T10:15:30")
    private LocalDateTime createdAt;

}
