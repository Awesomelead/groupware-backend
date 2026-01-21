package kr.co.awesomelead.groupware_backend.domain.visit.enums;

import com.fasterxml.jackson.annotation.JsonValue;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum VisitPurpose {
    CUSTOMER_INSPECTION("고객 검수"),
    HAZARDOUS_SUBSTANCE("유해화학물질"), // GOODS_DELIVERY에서 변경
    FACILITY_CONSTRUCTION("시설공사"),
    LOGISTICS_AND_DELIVERY("입출고 및 물품 납품"), // LOGISTICS에서 변경
    MEETING("미팅"),
    OTHER("기타");

    private final String description; // 한글 설명

    @JsonValue
    public String getDescription() {
        return description;
    }
}
