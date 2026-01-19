package kr.co.awesomelead.groupware_backend.domain.requesthistory.enums;

import com.fasterxml.jackson.annotation.JsonValue;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum RequestType {
    EMPLOYMENT_CERTIFICATE("재직증명서"), // 재직 중임을 증명
    CAREER_CERTIFICATE("경력증명서"), // 과거 경력 사항을 증명

    WITHHOLDING_RECEIPT("원천징수영수증"), // 연말정산용 근로소득 원천징수영수증
    INCOME_TAX_WITHHOLDING("갑종근로소득 원천징수명세서"), // 갑근세 원천징수 확인서
    RETIREMENT_CERTIFICATE("퇴직증명서"), // 퇴직 사실 증명

    ETC("기타"); // 그 외 요청 사항

    private final String description;

    @JsonValue
    public String getDescription() {
        return description;
    }
}
