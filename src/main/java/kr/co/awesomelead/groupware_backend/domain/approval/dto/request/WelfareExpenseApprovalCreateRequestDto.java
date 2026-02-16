package kr.co.awesomelead.groupware_backend.domain.approval.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Schema(description = "기안 및 지출결의(복리후생) 생성 요청 DTO")
public class WelfareExpenseApprovalCreateRequestDto extends ExpenseDraftApprovalCreateRequestDto {

}
