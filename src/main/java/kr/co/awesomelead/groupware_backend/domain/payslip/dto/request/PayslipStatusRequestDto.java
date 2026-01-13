package kr.co.awesomelead.groupware_backend.domain.payslip.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import kr.co.awesomelead.groupware_backend.domain.payslip.enums.PayslipStatus;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Schema(description = "급여명세서 상태 변경 요청")
public class PayslipStatusRequestDto {

    @Schema(description = "변경할 상태 (APPROVED 또는 REJECTED)", example = "REJECTED")
    @NotNull(message = "상태값은 필수입니다.")
    private PayslipStatus status;

    @Schema(description = "거절 사유 (상태가 REJECTED인 경우 필수)", example = "연장근로 수당이 잘못 계산되었습니다.")
    private String rejectionReason;

}
