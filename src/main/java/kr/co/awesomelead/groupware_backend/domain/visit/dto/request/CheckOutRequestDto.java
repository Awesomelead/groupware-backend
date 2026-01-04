package kr.co.awesomelead.groupware_backend.domain.visit.dto.request;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
@NoArgsConstructor
public class CheckOutRequestDto {

    private Long visitId;
    private LocalDateTime checkOutTime;
}
