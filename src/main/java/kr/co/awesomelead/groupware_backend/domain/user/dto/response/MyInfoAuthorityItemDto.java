package kr.co.awesomelead.groupware_backend.domain.user.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MyInfoAuthorityItemDto {

    @Schema(description = "권한 코드", example = "SEND_NOTIFICATION")
    private String code;

    @Schema(description = "권한 라벨", example = "알림 전송")
    private String label;

    @Schema(description = "권한 설명", example = "사용자에게 알림을 전송할 수 있습니다.")
    private String description;

    @Schema(description = "보유 여부", example = "true")
    private boolean enabled;
}
