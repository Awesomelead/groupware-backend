package kr.co.awesomelead.groupware_backend.domain.auth;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.validation.Validation;
import jakarta.validation.Validator;

import kr.co.awesomelead.groupware_backend.domain.auth.dto.request.SignupRequestDto;
import kr.co.awesomelead.groupware_backend.domain.department.enums.Company;

import org.junit.jupiter.api.Test;

class SignupRequestDtoTest {

    private final Validator validator =
            Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void 상세주소는_입력하지_않아도_유효하다() {
        SignupRequestDto requestDto = createValidRequest();
        requestDto.setAddress2(null);

        assertThat(validator.validate(requestDto)).isEmpty();
    }

    private SignupRequestDto createValidRequest() {
        SignupRequestDto requestDto = new SignupRequestDto();
        requestDto.setNameKor("홍길동");
        requestDto.setNameEng("Gildong Hong");
        requestDto.setNationality("대한민국");
        requestDto.setZipcode("06234");
        requestDto.setAddress1("서울특별시 강남구 테헤란로 123");
        requestDto.setCompany(Company.AWESOME);
        requestDto.setRegistrationNumber("9001011234567");
        requestDto.setPhoneNumber("01012345678");
        requestDto.setEmail("test@example.com");
        requestDto.setPassword("test1234!");
        requestDto.setPasswordConfirm("test1234!");
        return requestDto;
    }
}
