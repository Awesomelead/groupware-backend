package kr.co.awesomelead.groupware_backend.domain.user.enums;

import com.fasterxml.jackson.annotation.JsonValue;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.text.Collator;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

@Getter
@AllArgsConstructor
public enum Authority {
    // 전체 권한은 ADMIN으로 변경할 때, 자동 부여
    SEND_NOTIFICATION("알림 전송"), // jobType이 관리직일 경우 부여하고 시작
    MANAGE_DEPARTMENT_EDUCATION("부서 교육 관리"), // jobType이 관리직일 경우 부여하고 시작
    MANAGE_PSM("PSM 관리"),
    MANAGE_SAFETY("안전 보건 관리"),
    ACCESS_NOTICE("공지사항 관리"),

    MANAGE_VISITOR("내방객 관리"),

    EDIT_EMPLOYEE_INFO("직원 관리"),
    MANAGE_ANNUAL_LEAVE("연차 관리"),
    MANAGE_PAYSLIP("급여명세서 관리"),
    MANAGE_CERTIFICATE_REQUEST("제증명 발급 관리"),
    MANAGE_APPROVAL_LINE("결재선 설정 관리");

    private final String description;

    public static List<Authority> sortedByDescription() {
        return Arrays.stream(values()).sorted(descriptionComparator()).toList();
    }

    public static Comparator<Authority> descriptionComparator() {
        Collator collator = Collator.getInstance(Locale.KOREAN);
        return (left, right) -> {
            int result = collator.compare(left.getDescription(), right.getDescription());
            return result != 0 ? result : left.name().compareTo(right.name());
        };
    }

    @JsonValue
    public String getDescription() {
        return description;
    }
}
