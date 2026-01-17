package kr.co.awesomelead.groupware_backend.domain.education.enums;

import com.fasterxml.jackson.annotation.JsonValue;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum EduType {
    PSM("PSM"), // 전사/안전 관련 교육
    SAFETY("안전 보건"), // 전사/안전 관련 교육
    DEPARTMENT("부서 교육"); // 특정 부서 대상 교육 (부서 ID 필수)

    private final String description;

    @JsonValue
    public String getDescription() {
        return description;
    }
}
