package kr.co.awesomelead.groupware_backend.domain.requesthistory.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum RequestType {
    EMPLOYMENT_CERTIFICATE("재직증명서"), // 재직 중임을 증명
    CAREER_CERTIFICATE("경력증명서"); // 과거 경력 사항을 증명

    private final String description;
}
