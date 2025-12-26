package kr.co.awesomelead.groupware_backend.domain.visit.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class VisitorSearchRequestDto {

    @NotBlank
    private String name;

    @NotBlank
    @Pattern(regexp = "^01(?:0|1|[6-9])(?:\\d{3}|\\d{4})\\d{4}$", message = "올바른 전화번호 형식이 아닙니다.")
    private String phoneNumber;

    @NotBlank
    @Size(min = 4, max = 4, message = "비밀번호는 4자리여야 합니다.")
    private String password;
}
