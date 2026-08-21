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
    SEND_NOTIFICATION("알림 전송", "사용자에게 알림을 전송할 수 있습니다."),
    MANAGE_DEPARTMENT_EDUCATION("부서 교육 관리", "부서교육 전체 목록을 조회하고 생성, 수정, 삭제할 수 있습니다."),
    MANAGE_PSM("PSM 관리", "PSM 교육 전체 목록을 조회하고 생성, 수정, 삭제할 수 있습니다."),
    MANAGE_SAFETY("안전 보건 관리", "안전보건 교육 전체 목록을 조회하고 생성, 수정, 삭제할 수 있습니다."),
    ACCESS_NOTICE("공지사항 관리", "공지를 생성, 수정, 삭제할 수 있고 내가 작성한 공지를 조회할 수 있습니다."),
    VIEW_ALL_NOTICE("공지사항 전체 조회", "공지 대상자가 아니어도 모든 회사와 모든 대상의 공지를 조회할 수 있습니다."),

    MANAGE_VISITOR("내방객 관리", "내방객 신청, 방문 기록, 출입 상태를 조회하고 관리할 수 있습니다."),

    EDIT_EMPLOYEE_INFO("직원 관리", "직원 정보를 조회, 수정하고 권한, 역할, 계정 상태를 관리할 수 있습니다."),
    MANAGE_ANNUAL_LEAVE("연차 관리", "직원 연차를 생성, 수정, 삭제하고 관리자 연차 목록을 조회할 수 있습니다."),
    MANAGE_PAYSLIP("급여명세서 관리", "급여명세서를 생성, 수정, 삭제, 발송하고 관리자 목록을 조회할 수 있습니다."),
    MANAGE_CERTIFICATE_REQUEST("제증명 발급 관리", "제증명 발급 요청을 조회, 처리하고 발급 이력을 관리할 수 있습니다."),
    MANAGE_APPROVAL_LINE("결재선 설정 관리", "개발중입니다.");

    private final String label;
    private final String description;

    public static List<Authority> sortedByDescription() {
        return Arrays.stream(values()).sorted(descriptionComparator()).toList();
    }

    public static Comparator<Authority> descriptionComparator() {
        Collator collator = Collator.getInstance(Locale.KOREAN);
        return (left, right) -> {
            int result = collator.compare(left.getLabel(), right.getLabel());
            return result != 0 ? result : left.name().compareTo(right.name());
        };
    }

    @JsonValue
    public String getLabel() {
        return label;
    }
}
