package kr.co.awesomelead.groupware_backend.global.common.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Schema(description = "공통 분류 코드 응답 객체")
public class EnumCodeDto {

    @Schema(description = "Enum 영문 상수 값", example = "CHUNGNAM_HQ")
    private String code;

    @Schema(description = "Enum 한글 설명문", example = "충남사업본부")
    private String description;

    public static EnumCodeDto of(String code, String description) {
        return EnumCodeDto.builder().code(code).description(description).build();
    }
}
