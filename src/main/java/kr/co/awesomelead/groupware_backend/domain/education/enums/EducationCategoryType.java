package kr.co.awesomelead.groupware_backend.domain.education.enums;

import com.fasterxml.jackson.annotation.JsonValue;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum EducationCategoryType {
    PSM("PSM"),
    SAFETY("안전 보건");

    private final String description;

    @JsonValue
    public String getDescription() {
        return description;
    }
}
