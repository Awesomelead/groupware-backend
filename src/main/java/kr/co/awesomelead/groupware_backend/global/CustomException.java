package kr.co.awesomelead.groupware_backend.global;

import lombok.Getter;

@Getter
public class CustomException extends RuntimeException {

    private final ErrorCode errorCode;

    public CustomException(ErrorCode errorCode) {
        // 부모 생성자에 에러 메시지를 전달할 수도 있고, 안 할 수도 있습니다.
        // super(errorCode.getMessage());
        this.errorCode = errorCode;
    }
}
