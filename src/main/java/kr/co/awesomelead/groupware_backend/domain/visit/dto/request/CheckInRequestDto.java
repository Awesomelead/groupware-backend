package kr.co.awesomelead.groupware_backend.domain.visit.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.web.multipart.MultipartFile;

@Getter
@Setter // 파일 바인딩을 위해 필요
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "사전 예약자 입실 처리 요청 DTO")
public class CheckInRequestDto {

    @NotNull(message = "방문 ID는 필수입니다.")
    @Schema(description = "방문 신청 ID", example = "1")
    private Long visitId;

    @NotBlank(message = "비밀번호는 필수입니다.")
    @Size(min = 4, max = 4)
    @Schema(description = "신청 시 설정한 비밀번호 4자리", example = "1234")
    private String password;

    @NotNull(message = "방문자 서명은 필수입니다.")
    @Schema(description = "방문자 서명 png 이미지 파일")
    private MultipartFile signatureFile;
}
