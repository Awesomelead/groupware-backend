package kr.co.awesomelead.groupware_backend.domain.auth.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "본인인증 결과")
public class IdentityVerificationResponseDto {

    @Schema(description = "본인인증 ID", example = "identity-1768232040185")
    private String identityVerificationId;

    @Schema(description = "인증 상태", example = "VERIFIED")
    private String status;

    @Schema(description = "이름", example = "홍길동")
    private String name;

    @Schema(description = "전화번호", example = "01012345678")
    private String phoneNumber;

    @Schema(description = "생년월일", example = "1990-01-01")
    private String birthDate;

    @Schema(description = "성별", example = "MALE")
    private String gender;
}