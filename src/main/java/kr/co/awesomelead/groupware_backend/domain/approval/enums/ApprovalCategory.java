package kr.co.awesomelead.groupware_backend.domain.approval.enums;

import com.fasterxml.jackson.annotation.JsonValue;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ApprovalCategory {
    ALL("전체"),
    IN_PROGRESS("결재 진행"),
    REFERENCE("참조/열람"),
    DRAFT("내 기안");

    private final String description;

    @JsonValue
    public String getDescription() {
        return description;
    }
}
