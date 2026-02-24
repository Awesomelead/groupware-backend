package kr.co.awesomelead.groupware_backend.domain.user.enums;

import com.fasterxml.jackson.annotation.JsonValue;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum Authority {
    // 전체 권한은 ADMIN으로 변경할 때, 자동 부여
    ACCESS_MESSAGE("메세지 작성"), // jobType이 관리직일 경우 부여하고 시작
    ACCESS_EDUCATION("교육 작성"), // jobType이 관리직일 경우 부여하고 시작
    ACCESS_NOTICE("공지 작성"),

    ACCESS_VISIT("방문자 관리 접근"),

    MANAGE_EMPLOYEE_DATA("사원 데이터 관리"); // 연차, 급여명세서, 근태확인표 발송 권한

    private final String description;

    @JsonValue
    public String getDescription() {
        return description;
    }
}
