package kr.co.awesomelead.groupware_backend.domain.department.enums;

import com.fasterxml.jackson.annotation.JsonValue;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;

@Getter
@AllArgsConstructor
public enum DepartmentName {

    // Level 0: 최상위 부서
    CHUNGNAM_HQ("충남사업본부"),

    // Level 1: 충남사업본부 하위
    MARUI_LAB("(주)한국마루이 연구소"),
    AWESOME_LAB("(주)어썸리드 연구소"),
    SALES_DEPT("영업부"),
    CHUNGNAM_PLANNING("충남 경영기획실"),
    AWESOME_PROD_HQ("(주)어썸리드 생산본부"),
    MARUI_PROD_HQ("(주)한국마루이 생산본부"),
    TECHNICAL_ADVISOR("기술고문"),
    ENVIRONMENT_SAFETY("환경안전부"),
    QUALITY_CONTROL("품질관리부"),
    AWESOME_SECURITY_DEPT("어썸리드 경비"),
    MARUI_SECURITY_DEPT("마루이 경비"),

    // Level 2: 하위 부서들
    MANAGEMENT_SUPPORT("경영지원부"),
    CHAMBER_PROD("챔버생산부"),
    PARTS_PROD("부품생산부"),
    PRODUCTION("생산부");

    private final String description;

    @JsonValue
    public String getDescription() {
        return description;
    }

    /** 한글 부서명으로 Enum 상수를 찾는 편의 메서드 */
    public static DepartmentName fromDescription(String description) {
        return Arrays.stream(DepartmentName.values())
                .filter(v -> v.getDescription().equals(description))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("일치하는 부서명이 없습니다: " + description));
    }
}
