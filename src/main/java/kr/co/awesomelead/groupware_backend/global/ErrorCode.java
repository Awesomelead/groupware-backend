package kr.co.awesomelead.groupware_backend.global;

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

    // 401 Unauthorized
    INVALID_TOKEN(HttpStatus.UNAUTHORIZED, "유효하지 않은 토큰입니다."),
    EXPIRED_TOKEN(HttpStatus.UNAUTHORIZED, "만료된 토큰입니다."),
    AUTH_CODE_EXPIRED(HttpStatus.UNAUTHORIZED, "인증번호가 만료되었습니다."),
    VISITOR_AUTHENTICATION_FAILED(HttpStatus.UNAUTHORIZED, "내방객 인증에 실패했습니다."),

    // 404 Not Found
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "해당 사용자를 찾을 수 없습니다."),
    VISIT_NOT_FOUND(HttpStatus.NOT_FOUND, "해당 방문정보를 찾을 수 없습니다."),
    VISITOR_NOT_FOUND(HttpStatus.NOT_FOUND, "해당 내방객을 찾을 수 없습니다."),

    // 409 Conflict
    DUPLICATED_SIGNUP_REQUEST(HttpStatus.CONFLICT, "이미 처리된 가입 요청입니다."),
    DUPLICATE_LOGIN_ID(HttpStatus.CONFLICT, "이미 사용 중인 아이디입니다."),
    DUPLICATE_PHONE_NUMBER(HttpStatus.CONFLICT, "이미 가입된 전화번호입니다."),
    DUPLICATE_REGISTRATION_NUMBER(HttpStatus.CONFLICT, "이미 가입된 주민등록번호입니다."),

    // 500 Internal Server Error
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "서버 내부 오류가 발생했습니다."),
    ALIMTALK_SEND_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "알림톡 전송에 실패했습니다."),
    EMAIL_SEND_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "이메일 전송에 실패했습니다.");

    private final HttpStatus httpStatus;
    private final String message;
}
