package kr.co.awesomelead.groupware_backend.domain.annualleave.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AnnualLeaveResponseDto {

    private Double total; // 발생 연차
    private Double monthlyLeave; // 월차
    private Double carriedOver; // 이월 월차
    private Double used; // 사용
    private Double remain; // 잔여일
    private LocalDate updateDate; // 수정일
}
