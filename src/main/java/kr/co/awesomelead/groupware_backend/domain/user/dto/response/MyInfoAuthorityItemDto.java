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

    @Schema(description = "권한 코드", example = "ACCESS_MESSAGE")
    private String code;

    @Schema(description = "권한 라벨", example = "메세지 작성")
    private String label;

    @Schema(description = "보유 여부", example = "true")
    private boolean enabled;
}
