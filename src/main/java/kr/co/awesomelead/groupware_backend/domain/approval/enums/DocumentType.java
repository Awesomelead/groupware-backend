package kr.co.awesomelead.groupware_backend.domain.approval.enums;

import com.fasterxml.jackson.annotation.JsonValue;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum DocumentType {
    BASIC("기본양식", RetentionPeriod.FIVE_YEARS),
    LEAVE("근태신청서", RetentionPeriod.PERMANENT),
    OVERSEAS_TRIP("국외출장여비정산서", RetentionPeriod.PERMANENT),
    EXPENSE_DRAFT("기안및지출결의", RetentionPeriod.PERMANENT),
    WELFARE_EXPENSE("기안및지출결의_복리후생", RetentionPeriod.PERMANENT),
    CAR_FUEL("차량유류정산지출결의", RetentionPeriod.PERMANENT);

    private final String description;
    private final RetentionPeriod retentionPeriod;

    @JsonValue
    public String getDescription() {
        return description;
    }
}
