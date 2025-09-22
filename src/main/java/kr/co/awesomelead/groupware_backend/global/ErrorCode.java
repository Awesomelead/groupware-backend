package kr.co.awesomelead.groupware_backend.global;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {

    // 400 Bad Request
    INVALID_ARGUMENT(HttpStatus.BAD_REQUEST, "유효하지 않은 ARGUMENT입니다."),

    // 401 Unauthorized
    INVALID_TOKEN(HttpStatus.UNAUTHORIZED, "유효하지 않은 토큰입니다."),
    EXPIRED_TOKEN(HttpStatus.UNAUTHORIZED, "만료된 토큰입니다."),

    // 404 Not Found
    USER_NOT_FOUND(HttpStatus.NOT_FOUND,  "해당 사용자를 찾을 수 없습니다."),

    // 409 Conflict
    DUPLICATED_SIGNUP_REQUEST(HttpStatus.CONFLICT, "이미 처리된 가입 요청입니다."),
    DUPLICATE_LOGIN_ID(HttpStatus.CONFLICT,  "이미 사용 중인 아이디입니다."),

    // 500 Internal Server Error
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR,  "서버 내부 오류가 발생했습니다.");


    private final HttpStatus httpStatus;
    private final String message;
}