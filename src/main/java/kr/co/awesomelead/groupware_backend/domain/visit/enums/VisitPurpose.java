package kr.co.awesomelead.groupware_backend.domain.visit.enums;

import com.fasterxml.jackson.annotation.JsonValue;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum VisitPurpose {
    CUSTOMER_INSPECTION("고객 검수"),
    GOODS_DELIVERY("물품 납품"),
    FACILITY_CONSTRUCTION("시설공사"),
    LOGISTICS("입출고"),
    MEETING("미팅"),
    OTHER("기타");

    private final String description; // 한글 설명

    @JsonValue
    public String getDescription() {
        return description;
    }
}
