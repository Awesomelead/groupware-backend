package kr.co.awesomelead.groupware_backend.test.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
@Schema(description = "더미 유저 생성 결과")
public class DummyUsersCreateResponseDto {

    @Schema(description = "요청 생성 수", example = "20")
    private int requestedCount;

    @Schema(description = "실제 생성 수", example = "20")
    private int createdCount;

    @Schema(description = "중복 등으로 건너뛴 수", example = "0")
    private int skippedCount;
}
