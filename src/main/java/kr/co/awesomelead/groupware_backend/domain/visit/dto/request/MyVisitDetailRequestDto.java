package kr.co.awesomelead.groupware_backend.domain.visit.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "내 방문 상세 조회 요청")
public class MyVisitDetailRequestDto {

    @NotBlank(message = "비밀번호는 필수입니다.")
    @Size(min = 4, max = 4)
    @Schema(description = "조회용 비밀번호 (4자리)", example = "1234")
    private String password;
}
