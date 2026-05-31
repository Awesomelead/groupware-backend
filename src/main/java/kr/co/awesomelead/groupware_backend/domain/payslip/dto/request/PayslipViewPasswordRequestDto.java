package kr.co.awesomelead.groupware_backend.domain.payslip.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;

import jakarta.validation.constraints.NotBlank;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Schema(description = "급여명세서 열람 비밀번호 확인 요청")
public class PayslipViewPasswordRequestDto {

    @NotBlank(message = "비밀번호는 필수입니다.")
    @Schema(description = "서비스 로그인 비밀번호", example = "MyP@ssw0rd!")
    private String password;
}
