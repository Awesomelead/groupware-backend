package kr.co.awesomelead.groupware_backend.domain.visit.dto.request;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@AllArgsConstructor
@NoArgsConstructor
public class CheckOutRequestDto {

    private Long visitId;
    private LocalDateTime checkOutTime;

}
