package kr.co.awesomelead.groupware_backend.domain.visit.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Schema(description = "동행자 정보")
public class CompanionRequestDto {

    @NotBlank
    @Schema(description = "동행자 이름", example = "김철수", required = true)
    private String name;

    @NotBlank
    @Pattern(regexp = "^01(?:0|1|[6-9])(?:\\d{3}|\\d{4})\\d{4}$", message = "올바른 전화번호 형식이 아닙니다.")
    @Schema(description = "동행자 전화번호 (하이픈 제외)", example = "01087654321", required = true)
    private String phoneNumber;

    @Schema(description = "동행자 회사명", example = "테스트회사")
    private String visitorCompany;
}
