package kr.co.awesomelead.groupware_backend.domain.auth.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import kr.co.awesomelead.groupware_backend.domain.department.enums.Company;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Schema(description = "회원가입 요청")
public class SignupRequestDto {

    @Schema(description = "한글 이름", example = "홍길동", required = true)
    @NotBlank(message = "성명은 필수입니다.")
    private String nameKor;

    @Schema(description = "영문 이름", example = "Hong Gildong", required = true)
    @NotBlank(message = "영문 이름은 필수입니다.")
    private String nameEng;

    @Schema(description = "국적", example = "대한민국", required = true)
    @NotBlank(message = "국적은 필수입니다.")
    private String nationality;

    @Schema(
            description = "근무 사업장",
            example = "AWESOME",
            required = true,
            implementation = Company.class)
    @NotNull(message = "근무사업장은 필수입니다.")
    private Company company;

    @Schema(description = "주민등록번호 또는 외국인등록번호", example = "900101-1234567", required = true)
    @NotBlank(message = "주민등록번호(또는 외국인번호)는 필수입니다.")
    @Pattern(regexp = "^\\d{6}-[1-8]\\d{6}$", message = "주민등록번호 형식(YYMMDD-GXXXXXX)이 올바르지 않습니다.")
    private String registrationNumber;

    @Schema(description = "전화번호 ('-' 없이 10~11자리)", example = "01012345678", required = true)
    @NotBlank(message = "전화번호는 필수입니다.")
    @Pattern(regexp = "^\\d{10,11}$", message = "전화번호는 '-' 없이 10~11자리 숫자로 입력해주세요.")
    private String phoneNumber;

    @Schema(description = "이메일", example = "test@example.com", required = true)
    @NotBlank(message = "이메일은 필수입니다.")
    @Email(message = "유효한 이메일 형식이 아닙니다.")
    private String email;

    @Schema(description = "비밀번호 (영문, 숫자, 특수문자 포함 8자 이상)", example = "test1234!", required = true)
    @NotBlank(message = "비밀번호는 필수입니다.")
    @Size(min = 8, message = "비밀번호는 최소 8자 이상이어야 합니다.")
    @Pattern(
            regexp = "^(?=.*[a-zA-Z])(?=.*[!@#$%^*+=-])(?=.*[0-9]).{8,64}$",
            message = "비밀번호는 영문, 숫자, 특수문자를 모두 포함해야 합니다.")
    private String password;

    @Schema(description = "비밀번호 확인", example = "test1234!", required = true)
    @NotBlank(message = "비밀번호 확인은 필수입니다.")
    private String passwordConfirm;
}
