package kr.co.awesomelead.groupware_backend.domain.approval.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Schema(description = "기안 및 지출결의(복리후생) 생성 요청 DTO")
public class WelfareExpenseApprovalCreateRequestDto extends ExpenseDraftApprovalCreateRequestDto {

    @Schema(description = "합의부서 또는 수신부서명", example = "경영지원팀")
    @NotBlank(message = "합의부서(또는 수신부서)는 필수입니다.")
    private String agreementDepartment; // 복리후생 기안에만 필요한 합의부서/수신부서
}
