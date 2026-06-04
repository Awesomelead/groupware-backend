package kr.co.awesomelead.groupware_backend.domain.requesthistory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

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
    @DisplayName("convertToEntityAttribute 메서드는 enum 코드를 enum으로 변환한다")
    void convertToEntityAttribute_supports_enum_name() {
        assertThat(converter.convertToEntityAttribute("GOVERNMENT_OFFICE"))
                .isEqualTo(RequestPurpose.GOVERNMENT_OFFICE);
    }

    @Test
    @DisplayName("convertToEntityAttribute 메서드는 현재 한글 표시값을 enum으로 변환한다")
    void convertToEntityAttribute_supports_current_description() {
        assertThat(converter.convertToEntityAttribute("금융기관 제출용"))
                .isEqualTo(RequestPurpose.FINANCIAL_INSTITUTION);
    }

    @Test
    @DisplayName("convertToEntityAttribute 메서드는 기존 호환 값을 변환하지 않는다")
    void convertToEntityAttribute_rejects_legacy_values() {
        assertThatThrownBy(() -> converter.convertToEntityAttribute("관공서_제출용"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> converter.convertToEntityAttribute("은행 제출용"))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
