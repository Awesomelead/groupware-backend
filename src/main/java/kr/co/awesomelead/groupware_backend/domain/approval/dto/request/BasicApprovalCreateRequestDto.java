package kr.co.awesomelead.groupware_backend.domain.approval.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Schema(description = "기본 기안문 생성 요청 DTO")
public class BasicApprovalCreateRequestDto extends ApprovalCreateRequestDto {

}
