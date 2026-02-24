package kr.co.awesomelead.groupware_backend.domain.approval.enums;

import com.fasterxml.jackson.annotation.JsonValue;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ApprovalCategory {
    ALL("전체"),
    IN_PROGRESS("결재진행"),
    REFERENCE("참조문서"),
    DRAFT("내 작성");

    private final String description;

    @JsonValue
    public String getDescription() {
        return description;
    }
}
