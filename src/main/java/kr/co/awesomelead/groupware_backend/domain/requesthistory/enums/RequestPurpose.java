package kr.co.awesomelead.groupware_backend.domain.requesthistory.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;

@Getter
@AllArgsConstructor
public enum RequestPurpose {
    FINANCIAL_INSTITUTION("금융기관 제출용"),
    GOVERNMENT_OFFICE("관공서 제출용"),
    OTHER_INSTITUTION("타기관 제출용"),
    EMPLOYMENT_CAREER_CONFIRMATION("재직, 경력 확인용"),
    ETC("기타");

    private final String description;

    @JsonValue
    public String getDescription() {
        return description;
    }

    @JsonCreator
    public static RequestPurpose from(String value) {
        if (value == null) {
            return null;
        }

        String trimmedValue = value.trim();

        return Arrays.stream(values())
                .filter(
                        purpose ->
                                purpose.name().equalsIgnoreCase(trimmedValue)
                                        || purpose.getDescription().equals(trimmedValue))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("알 수 없는 제증명 발급 용도: " + value));
    }
}
