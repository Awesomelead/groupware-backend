package kr.co.awesomelead.groupware_backend.domain.requesthistory;

import static org.assertj.core.api.Assertions.assertThat;

import kr.co.awesomelead.groupware_backend.domain.requesthistory.converter.RequestPurposeConverter;
import kr.co.awesomelead.groupware_backend.domain.requesthistory.enums.RequestPurpose;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("RequestPurposeConverter 클래스의")
class RequestPurposeConverterTest {

    private final RequestPurposeConverter converter = new RequestPurposeConverter();

    @Test
    @DisplayName("convertToDatabaseColumn 메서드는 enum 코드를 저장값으로 반환한다")
    void convertToDatabaseColumn_returns_enum_name() {
        assertThat(converter.convertToDatabaseColumn(RequestPurpose.GOVERNMENT_OFFICE))
                .isEqualTo("GOVERNMENT_OFFICE");
    }

    @Test
    @DisplayName("convertToEntityAttribute 메서드는 언더바가 포함된 기존 한글 값을 enum으로 변환한다")
    void convertToEntityAttribute_supports_legacy_underscore_value() {
        assertThat(converter.convertToEntityAttribute("관공서_제출용"))
                .isEqualTo(RequestPurpose.GOVERNMENT_OFFICE);
    }

    @Test
    @DisplayName("convertToEntityAttribute 메서드는 기존 은행 제출용 값을 금융기관 제출용으로 변환한다")
    void convertToEntityAttribute_supports_legacy_bank_value() {
        assertThat(converter.convertToEntityAttribute("은행 제출용"))
                .isEqualTo(RequestPurpose.FINANCIAL_INSTITUTION);
    }
}
