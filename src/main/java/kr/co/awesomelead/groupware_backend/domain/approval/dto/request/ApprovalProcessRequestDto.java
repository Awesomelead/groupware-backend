package kr.co.awesomelead.groupware_backend.domain.approval.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Schema(description = "결재 승인/반려 요청 DTO")
public class ApprovalProcessRequestDto {

    @Schema(description = "의견 (승인 시 선택, 반려 시 필수)", example = "검토 완료하였습니다.")
    private String comment;
}
