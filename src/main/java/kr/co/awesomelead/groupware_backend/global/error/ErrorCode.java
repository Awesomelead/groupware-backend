package kr.co.awesomelead.groupware_backend.global.error;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {

    // 400 Bad Request
    INVALID_ARGUMENT(HttpStatus.BAD_REQUEST, "유효하지 않은 ARGUMENT입니다."),
    AUTH_CODE_MISMATCH(HttpStatus.BAD_REQUEST, "인증번호가 일치하지 않습니다."),
    PHONE_NOT_VERIFIED(HttpStatus.BAD_REQUEST, "전화번호 인증이 필요합니다."),
    EMAIL_NOT_VERIFIED(HttpStatus.BAD_REQUEST, "이메일 인증이 필요합니다."),
    PASSWORD_MISMATCH(HttpStatus.BAD_REQUEST, "비밀번호가 일치하지 않습니다."),
    VISIT_ALREADY_CHECKED_OUT(HttpStatus.BAD_REQUEST, "이미 체크아웃된 방문정보입니다."),
    VISITOR_PASSWORD_REQUIRED_FOR_PRE_REGISTRATION(
            HttpStatus.BAD_REQUEST, "사전 방문 예약 시 내방객 비밀번호가 필요합니다."),
    DEPARTMENT_ID_REQUIRED(HttpStatus.BAD_REQUEST, "부서교육인 경우 부서 ID가 필요합니다."),
    ALREADY_MARKED_ATTENDANCE(HttpStatus.BAD_REQUEST, "이미 출석이 체크된 교육입니다."),
    NO_SIGNATURE_PROVIDED(HttpStatus.BAD_REQUEST, "서명이 제공되지 않았습니다."),
    INVALID_SIGNATURE_FORMAT(HttpStatus.BAD_REQUEST, "서명은 PNG 파일 형식만 지원합니다."),
    CURRENT_PASSWORD_MISMATCH(HttpStatus.BAD_REQUEST, "현재 비밀번호가 일치하지 않습니다."),
    SAME_AS_CURRENT_PASSWORD(HttpStatus.BAD_REQUEST, "새 비밀번호는 현재 비밀번호와 달라야 합니다."),
    FILE_UPLOAD_ERROR(HttpStatus.BAD_REQUEST, "파일 업로드 중 오류가 발생했습니다."),
    INVALID_BASE_DATE_FORMAT(HttpStatus.BAD_REQUEST, "유효하지 않은 기준일자 형식입니다. (yyyy-MM-dd)"),
    ONLY_PDF_ALLOWED(HttpStatus.BAD_REQUEST, "PDF 파일 형식만 업로드할 수 있습니다."),
    IDENTITY_VERIFICATION_NOT_COMPLETED(HttpStatus.BAD_REQUEST, "본인인증이 완료되지 않았습니다."),
    IDENTITY_VERIFICATION_NOT_FOUND(HttpStatus.BAD_REQUEST, "해당 본인인증 정보를 찾을 수 없습니다."),
    NO_REJECTION_REASON_PROVIDED(HttpStatus.BAD_REQUEST, "반려 사유가 제공되지 않았습니다."),
    PHONE_NUMBER_MISMATCH(HttpStatus.BAD_REQUEST, "입력한 전화번호가 계정의 전화번호와 일치하지 않습니다."),
    PHONE_NUMBER_ALREADY_EXISTS(HttpStatus.BAD_REQUEST, "이미 사용 중인 전화번호입니다."),
    NAME_ENG_ALREADY_SAME(HttpStatus.BAD_REQUEST, "입력한 영문 이름이 현재 영문 이름과 동일합니다."),
    PHONE_NUMBER_ALREADY_SAME(HttpStatus.BAD_REQUEST, "입력한 전화번호가 현재 전화번호와 동일합니다."),
    PERMISSION_DETAIL_REQUIRED(HttpStatus.BAD_REQUEST, "기타 허가 선택 시 요구사항 작성이 필요합니다."),
    ADDITIONAL_PERMISSION_REQUIRED(HttpStatus.BAD_REQUEST, "시설공사 목적의 방문 시 추가 허가가 필요합니다."),
    INVALID_PASSWORD(HttpStatus.BAD_REQUEST, "유효하지 않은 비밀번호입니다."),
    NOT_VISIT_DATE(HttpStatus.BAD_REQUEST, "오늘 방문 일정이 아닙니다."),
    NOT_IN_PROGRESS(HttpStatus.BAD_REQUEST, "현재 방문 상태가 '방문 중'이 아닙니다."),
    NOT_LONG_TERM_VISIT(HttpStatus.BAD_REQUEST, "장기 방문 건이 아닙니다."),
    INVALID_VISIT_STATUS(HttpStatus.BAD_REQUEST, "승인 가능한 상태가 아닙니다."),
    INVALID_VISIT_DATE_RANGE(HttpStatus.BAD_REQUEST, "종료일은 시작일보다 빠를 수 없습니다."),
    INVALID_TIME_RANGE(HttpStatus.BAD_REQUEST, "퇴실 예정 시간은 입실 예정 시간보다 빠를 수 없습니다."),
    LONG_TERM_PERIOD_EXCEEDED(HttpStatus.BAD_REQUEST, "장기 방문은 최대 3개월까지만 신청 가능합니다."),
    INVALID_CHECKOUT_TIME(HttpStatus.BAD_REQUEST, "퇴실 시간은 입실 시간보다 빠를 수 없습니다."),
    INVALID_JOB_TYPE_FOR_ADMIN_ROLE(HttpStatus.BAD_REQUEST, "관리자 역할에는 관리직 직군만 할당할 수 있습니다."),
    REJECTION_REASON_REQUIRED(HttpStatus.BAD_REQUEST, "반려 시 반려 사유가 반드시 필요합니다."),
    INVALID_APPROVAL_STEP(HttpStatus.BAD_REQUEST, "결재선은 최소 한 명이 필요합니다."),
    ALREADY_PROCESSED_STEP(HttpStatus.BAD_REQUEST, "이미 처리된 결재 단계입니다."),
    NOT_YOUR_TURN(HttpStatus.BAD_REQUEST, "아직 본인의 결재 순서가 아닙니다."),
    NOT_APPROVER(HttpStatus.FORBIDDEN, "해당 결재 문서의 결재 대상자가 아닙니다."),
    APPROVAL_NOT_FOUND(HttpStatus.NOT_FOUND, "해당 결재 문서를 찾을 수 없습니다."),
    INVALID_LEAVE_DETAIL_TYPE(HttpStatus.BAD_REQUEST, "휴가 유형에 맞지 않는 소분류입니다."),
    DUPLICATE_APPROVER(HttpStatus.BAD_REQUEST, "동일한 결재자가 중복되어 있습니다."),
    AUTHORITY_ALREADY_ASSIGNED(HttpStatus.BAD_REQUEST, "이미 부여된 권한입니다."),
    AUTHORITY_NOT_ASSIGNED(HttpStatus.BAD_REQUEST, "부여되지 않은 권한은 제거할 수 없습니다."),
    MY_INFO_UPDATE_NO_CHANGES(HttpStatus.BAD_REQUEST, "변경 요청할 내 정보가 없습니다."),
    MY_INFO_UPDATE_ALREADY_PENDING(HttpStatus.BAD_REQUEST, "이미 처리 대기 중인 개인정보 수정 요청이 있습니다."),
    MY_INFO_UPDATE_REJECT_REASON_REQUIRED(HttpStatus.BAD_REQUEST, "반려 사유를 입력해주세요."),
    MY_INFO_UPDATE_REQUEST_NOT_CANCELABLE(HttpStatus.BAD_REQUEST, "대기 상태 요청만 취소할 수 있습니다."),

    // 401 Unauthorized
    INVALID_TOKEN(HttpStatus.UNAUTHORIZED, "유효하지 않은 토큰입니다."),
    EXPIRED_TOKEN(HttpStatus.UNAUTHORIZED, "만료된 토큰입니다."),
    AUTH_CODE_EXPIRED(HttpStatus.UNAUTHORIZED, "인증번호가 만료되었습니다."),
    VISITOR_AUTHENTICATION_FAILED(HttpStatus.UNAUTHORIZED, "내방객 인증에 실패했습니다."),
    NO_AUTHORITY_FOR_EDU_REPORT(HttpStatus.UNAUTHORIZED, "교육 보고서 관리 권한이 없습니다."),
    VISIT_ACCESS_DENIED(HttpStatus.UNAUTHORIZED, "해당 방문정보에 대한 접근 권한이 없습니다."),
    NO_AUTHORITY_FOR_NOTICE(HttpStatus.UNAUTHORIZED, "공지사항 작성 권한이 없습니다."),
    NO_AUTHORITY_FOR_ANNUAL_LEAVE(HttpStatus.UNAUTHORIZED, "연차 발송 권한이 없습니다."),
    NO_AUTHORITY_FOR_PAYSLIP(HttpStatus.UNAUTHORIZED, "급여명세서 발송 권한이 없습니다."),
    NO_AUTHORITY_FOR_REGISTRATION(HttpStatus.UNAUTHORIZED, "회원가입 승인 권한이 없습니다."),
    NO_AUTHORITY_FOR_ROLE_UPDATE(HttpStatus.UNAUTHORIZED, "사용자 역할 변경 권한이 없습니다."),
    NO_AUTHORITY_FOR_VIEW_PAYSLIP(HttpStatus.UNAUTHORIZED, "급여명세서 조회 권한이 없습니다."),
    NO_AUTHORITY_FOR_NOTIFICATION(HttpStatus.UNAUTHORIZED, "해당 알림에 대한 접근 권한이 없습니다."),

    // 403
    REFRESH_TOKEN_MISMATCH(HttpStatus.FORBIDDEN, "해당 리프레시 토큰에 대한 권한이 없습니다."),
    NO_AUTHORITY_FOR_EDU_REPORT(HttpStatus.FORBIDDEN, "교육 보고서 관리 권한이 없습니다."),
    VISIT_ACCESS_DENIED(HttpStatus.FORBIDDEN, "해당 방문정보에 대한 접근 권한이 없습니다."),
    NO_AUTHORITY_FOR_NOTICE(HttpStatus.FORBIDDEN, "공지사항 작성 권한이 없습니다."),
    NO_AUTHORITY_FOR_ANNUAL_LEAVE(HttpStatus.FORBIDDEN, "연차 발송 권한이 없습니다."),
    NO_AUTHORITY_FOR_PAYSLIP(HttpStatus.FORBIDDEN, "급여명세서 발송 권한이 없습니다."),
    NO_AUTHORITY_FOR_REGISTRATION(HttpStatus.FORBIDDEN, "회원가입 승인 권한이 없습니다."),
    NO_AUTHORITY_FOR_ROLE_UPDATE(HttpStatus.FORBIDDEN, "사용자 역할 변경 권한이 없습니다."),
    NO_AUTHORITY_FOR_VIEW_PAYSLIP(HttpStatus.FORBIDDEN, "급여명세서 조회 권한이 없습니다."),
    NO_AUTHORITY_FOR_MY_INFO_UPDATE_APPROVAL(HttpStatus.FORBIDDEN, "개인정보 수정 승인 권한이 없습니다."),
    NO_AUTHORITY_FOR_MY_INFO_UPDATE_CANCEL(HttpStatus.FORBIDDEN, "본인의 개인정보 수정 요청만 취소할 수 있습니다."),

    // 404 Not Found
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "해당 사용자를 찾을 수 없습니다."),
    VISIT_NOT_FOUND(HttpStatus.NOT_FOUND, "해당 방문정보를 찾을 수 없습니다."),
    VISITOR_NOT_FOUND(HttpStatus.NOT_FOUND, "해당 내방객을 찾을 수 없습니다."),
    DEPARTMENT_NOT_FOUND(HttpStatus.NOT_FOUND, "해당 부서를 찾을 수 없습니다."),
    EDU_REPORT_NOT_FOUND(HttpStatus.NOT_FOUND, "해당 교육 보고서를 찾을 수 없습니다."),
    EDU_ATTACHMENT_NOT_FOUND(HttpStatus.NOT_FOUND, "해당 교육 첨부파일을 찾을 수 없습니다."),
    NOTICE_NOT_FOUND(HttpStatus.NOT_FOUND, "해당 공지사항을 찾을 수 없습니다."),
    NOTICE_ATTACHMENT_NOT_FOUND(HttpStatus.NOT_FOUND, "해당 공지사항 첨부파일을 찾을 수 없습니다."),
    PAYSLIP_NOT_FOUND(HttpStatus.NOT_FOUND, "해당 급여명세서를 찾을 수 없습니다."),
    RECORD_NOT_FOUND(HttpStatus.NOT_FOUND, "해당 방문기록을 찾을 수 없습니다."),
    FCM_TOKEN_NOT_FOUND(HttpStatus.NOT_FOUND, "해당 FCM 토큰을 찾을 수 없습니다."),
    NOTIFICATION_NOT_FOUND(HttpStatus.NOT_FOUND, "해당 알림을 찾을 수 없습니다."),
    MY_INFO_UPDATE_REQUEST_NOT_FOUND(HttpStatus.NOT_FOUND, "해당 개인정보 수정 요청을 찾을 수 없습니다."),

    // 409 Conflict
    DUPLICATED_SIGNUP_REQUEST(HttpStatus.CONFLICT, "이미 처리된 가입 요청입니다."),
    DUPLICATE_LOGIN_ID(HttpStatus.CONFLICT, "이미 사용 중인 아이디입니다."),
    DUPLICATE_PHONE_NUMBER(HttpStatus.CONFLICT, "이미 가입된 전화번호입니다."),
    DUPLICATE_REGISTRATION_NUMBER(HttpStatus.CONFLICT, "이미 가입된 주민등록번호입니다."),

    // 500 Internal Server Error
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "서버 내부 오류가 발생했습니다."),
    ALIMTALK_SEND_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "알림톡 전송에 실패했습니다."),
    EMAIL_SEND_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "이메일 전송에 실패했습니다."),
    IDENTITY_VERIFICATION_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "본인인증 조회에 실패했습니다.");

    private final HttpStatus httpStatus;
    private final String message;
}
