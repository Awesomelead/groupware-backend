package kr.co.awesomelead.groupware_backend.domain.user.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Schema(description = "내 정보 수정 요청")
public class UpdateMyInfoRequestDto {

    @Schema(description = "영문 이름", example = "Kim Chulsoo")
    @Size(max = 50, message = "영문 이름은 최대 50자까지 입력 가능합니다.")
    private String nameEng;

    @Schema(description = "전화번호 ('-' 없이 10~11자리)", example = "01012345678")
    @Pattern(regexp = "^\\d{10,11}$", message = "전화번호는 '-' 없이 10~11자리 숫자로 입력해주세요.")
    private String phoneNumber;

    @Schema(description = "우편번호", example = "06234")
    @Size(max = 20, message = "우편번호는 최대 20자까지 입력 가능합니다.")
    private String zipcode;

    @Schema(description = "주소1", example = "서울특별시 강남구 테헤란로 123")
    @Size(max = 255, message = "주소1은 최대 255자까지 입력 가능합니다.")
    private String address1;

    @Schema(description = "주소2", example = "어썸리드빌딩 5층")
    @Size(max = 255, message = "주소2는 최대 255자까지 입력 가능합니다.")
    private String address2;
}
