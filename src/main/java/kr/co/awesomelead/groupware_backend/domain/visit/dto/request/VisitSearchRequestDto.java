package kr.co.awesomelead.groupware_backend.domain.visit.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "방문 정보 조회 요청 (내방객용)")
public class VisitSearchRequestDto {

    @NotBlank
    @Schema(description = "내방객 이름", example = "홍길동", required = true)
    private String name;

    @NotBlank
    @Pattern(regexp = "^01(?:0|1|[6-9])(?:\\d{3}|\\d{4})\\d{4}$", message = "올바른 전화번호 형식이 아닙니다.")
    @Schema(description = "내방객 전화번호 (하이픈 제외)", example = "01012345678", required = true)
    private String phoneNumber;

    @NotBlank
    @Size(min = 4, max = 4, message = "비밀번호는 4자리여야 합니다.")
    @Schema(description = "내방객 비밀번호 (4자리)", example = "1234", required = true, minLength = 4, maxLength = 4)
    private String password;
}