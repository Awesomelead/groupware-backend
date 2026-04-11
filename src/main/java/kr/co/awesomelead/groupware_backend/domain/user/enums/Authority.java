package kr.co.awesomelead.groupware_backend.domain.user.enums;

import com.fasterxml.jackson.annotation.JsonValue;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum Authority {
    // 전체 권한은 ADMIN으로 변경할 때, 자동 부여
    SEND_NOTIFICATION("알림 전송"), // jobType이 관리직일 경우 부여하고 시작
    WRITE_DEPARTMENT_EDUCATION("부서 교육 작성"), // jobType이 관리직일 경우 부여하고 시작
    WRITE_SAFETY("PSM/안전보건 작성"),
    ACCESS_NOTICE("공지 작성"),

    MANAGE_VISITOR("내방객 관리"),

    EDIT_EMPLOYEE_INFO("직원 정보 수정"), // 연차, 급여명세서, 근태확인표 발송 권한
    MANAGE_CERTIFICATE_REQUEST("제증명 신청 승인/반려"),
    MANAGE_APPROVAL_LINE("결재선 설정 관리");

    private final String description;

    @JsonValue
    public String getDescription() {
        return description;
    }
}
