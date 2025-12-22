package kr.co.awesomelead.groupware_backend.domain.education.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum EduType {
    PSM("PSM 교육"), // 전사/안전 관련 교육 (부서 무관)
    DEPARTMENT("부서 교육"); // 특정 부서 대상 교육 (부서 ID 필수)

    private final String description;
}
