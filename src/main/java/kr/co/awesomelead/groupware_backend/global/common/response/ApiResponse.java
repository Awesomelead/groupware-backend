package kr.co.awesomelead.groupware_backend.global.common.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
@JsonPropertyOrder({"isSuccess", "code", "message", "result"})
public class ApiResponse<T> {

    private final Boolean isSuccess;
    private final String code;
    private final String message;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private T result;

    // 성공 시 호출하는 정적 팩토리 메서드
    public static <T> ApiResponse<T> onSuccess(T result) {
        return new ApiResponse<>(true, "COMMON200", "요청에 성공했습니다.", result);
    }

    // 생성 201 Created (방문 신청, 공지 등록 등)
    public static <T> ApiResponse<T> onCreated(T result) {
        return new ApiResponse<>(true, "COMMON201", "성공적으로 생성되었습니다.", result);
    }

    // 삭제/빈 처리 204 No Content 대체
    public static <T> ApiResponse<T> onNoContent() {
        return new ApiResponse<>(true, "COMMON204", "성공적으로 처리되었습니다.", null);
    }

    public static <T> ApiResponse<T> onNoContent(String message) {
        return new ApiResponse<>(true, "COMMON204", message, null);
    }

    // 실패 시 호출하는 메서드 (보통 에러 코드를 상수로 관리)
    public static <T> ApiResponse<T> onFailure(String code, String message) {
        return new ApiResponse<>(false, code, message, null);
    }

    // 실패 시에도 데이터를 담을 수 있도록
    public static <T> ApiResponse<T> onFailure(String code, String message, T data) {
        return new ApiResponse<>(false, code, message, data);
    }
}
