package kr.co.awesomelead.groupware_backend.domain.notification.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 알림 메시지 템플릿을 관리하는 Enum.
 *
 * <p>title과 content는 String.format()으로 포맷팅 가능한 패턴을 사용할 수 있습니다. 예: {@code
 * NotificationMessage.SIGNUP_ADMIN_ALERT.formatContent("홍길동")}
 */
@Getter
@RequiredArgsConstructor
public enum NotificationMessage {
    SIGNUP_ADMIN_ALERT("신규 직원 가입 신청", "[%s] 님이 가입 신청을 완료했습니다. 계정 승인을 진행해 주세요."),
    NOTICE_CREATED("새 공지사항", "[%s] 공지사항이 등록되었습니다."),
    VISIT_ONE_DAY_PRE("방문 예정 안내", "[%s] 고객이 %s %s에 방문 예정입니다. (담당: %s)"),
    VISIT_LONG_TERM_PRE("장기 방문 승인 요청", "[%s] 고객이 %s~%s 장기 방문을 신청했습니다. 승인을 진행해 주세요."),
    VISIT_CHECK_IN("방문객 입실 안내", "[%s] 고객이 %s에 입실했습니다."),

    MY_INFO_UPDATE_REQUEST_ADMIN("내 정보 수정 승인 요청", "[%s] 님이 내 정보 수정을 요청했습니다. 승인을 진행해 주세요."),
    MY_INFO_UPDATE_APPROVED("내 정보 수정 승인 완료", "요청하신 내 정보 수정이 승인되었습니다."),
    MY_INFO_UPDATE_REJECTED("내 정보 수정 반려 안내", "요청하신 내 정보 수정이 반려되었습니다. (사유: %s)"),

    REQUEST_HISTORY_CREATED("증명서 발급 신청", "[%s] 증명서 발급 신청서가 제출되었습니다. 승인을 진행해 주세요."),
    REQUEST_HISTORY_ISSUED("증명서 발급 완료", "요청하신 증명서(%s)가 발급되었습니다."),
    REQUEST_HISTORY_REJECTED("증명서 발급 반려", "요청하신 증명서(%s) 발급이 반려되었습니다. (사유: %s)"),

    EDU_REPORT_CREATED("새 교육 등록", "[%s] 새로운 교육(%s)이 등록되었습니다."),

    ANNUAL_LEAVE_UPDATED("연차 갱신 안내", "%s 기준 연차가 반영되었습니다."),

    PAYSLIP_SENT("급여명세서 발송", "급여명세서가 발송되었습니다."),

    // 전자결재
    APPROVAL_CREATED_APPROVER("결재 요청", "[%s] 결재 요청이 도착했습니다."),
    APPROVAL_CREATED_REFERRER("결재 참조", "[%s] 문서가 참조되었습니다."),
    APPROVAL_REJECTED("결재 반려", "[%s] 문서가 반려되었습니다. 사유: %s"),
    APPROVAL_FINALLY_APPROVED("결재 최종 승인", "[%s] 문서가 최종 승인되었습니다.");

    private final String title;
    private final String contentPattern;

    /**
     * title을 그대로 반환합니다.
     */
    public String getTitle() {
        return title;
    }

    /**
     * contentPattern에 인자를 대입하여 최종 내용을 반환합니다.
     *
     * @param args String.format() 에 전달할 인자
     */
    public String formatContent(Object... args) {
        return String.format(contentPattern, args);
    }
}
