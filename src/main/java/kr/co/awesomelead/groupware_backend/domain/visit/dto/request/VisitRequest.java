package kr.co.awesomelead.groupware_backend.domain.visit.dto.request;

import kr.co.awesomelead.groupware_backend.domain.visit.enums.AdditionalPermissionType;
import kr.co.awesomelead.groupware_backend.domain.visit.enums.VisitPurpose;

public interface VisitRequest {

    String getVisitorName();

    String getVisitorPhoneNumber();

    String getVisitorCompany();

    String getCarNumber();

    VisitPurpose getPurpose();

    AdditionalPermissionType getPermissionType();

    String getPermissionDetail();

    Long getHostId();

    String getPassword();

}
