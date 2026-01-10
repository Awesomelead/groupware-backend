package kr.co.awesomelead.groupware_backend.domain.annualleave.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class ExcelUploadResponseDto {

    private int totalCount; // 전체 행 개수
    private int successCount; // 성공 개수
    private int failureCount; // 실패 개수
    private List<FailureDetail> failures; // 실패한 행들의 상세 정보

    @Getter
    @AllArgsConstructor
    public static class FailureDetail {

        private int rowNum; // 엑셀의 몇 번째 줄인지
        private String name; // 실패한 직원 이름
        private String reason; // 실패 원인 (예: "유저를 찾을 수 없음", "숫자 형식이 아님")
    }
}
