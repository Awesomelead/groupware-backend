package kr.co.awesomelead.groupware_backend.domain.visit.mapper;

import kr.co.awesomelead.groupware_backend.domain.user.entity.User;
import kr.co.awesomelead.groupware_backend.domain.visit.dto.request.CompanionRequestDto;
import kr.co.awesomelead.groupware_backend.domain.visit.dto.request.VisitCreateRequestDto;
import kr.co.awesomelead.groupware_backend.domain.visit.dto.response.MyVisitResponseDto;
import kr.co.awesomelead.groupware_backend.domain.visit.dto.response.VisitResponseDto;
import kr.co.awesomelead.groupware_backend.domain.visit.dto.response.VisitSummaryResponseDto;
import kr.co.awesomelead.groupware_backend.domain.visit.entity.Companion;
import kr.co.awesomelead.groupware_backend.domain.visit.entity.Visit;
import kr.co.awesomelead.groupware_backend.domain.visit.entity.Visitor;
import kr.co.awesomelead.groupware_backend.domain.visit.enums.VisitType;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface VisitMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "name", source = "dto.visitorName")
    @Mapping(target = "phoneNumber", source = "dto.visitorPhone")
    @Mapping(target = "password", source = "dto.visitorPassword")
    @Mapping(target = "visitInfos", ignore = true)
    Visitor toVisitorEntity(VisitCreateRequestDto dto);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "user", source = "host") // host 파라미터를 user 필드에 매핑
    @Mapping(target = "visitor", source = "visitor") // visitor 파라미터를 visitor 필드에 매핑
    @Mapping(target = "visitType", source = "type")
    @Mapping(target = "visitEndDate", ignore = true)
    @Mapping(target = "visited", expression = "java(type == VisitType.ON_SITE)")
    @Mapping(target = "verified", expression = "java(type == VisitType.ON_SITE)")
    @Mapping(target = "additionalRequirements", ignore = true)
    @Mapping(target = "signatureKey", ignore = true)
    @Mapping(target = "agreement", constant = "true")
    Visit toVisitEntity(VisitCreateRequestDto dto, User host, Visitor visitor, VisitType type);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "visit", ignore = true)
    Companion toCompanionEntity(CompanionRequestDto dto);

    @Mapping(target = "hostUserId", source = "user.id")
    @Mapping(target = "hostName", source = "user.nameKor")
    @Mapping(target = "hostDepartment", source = "user.department.name") // 필요 시 자동 매핑
    @Mapping(target = "visitorName", source = "visitor.name")
    @Mapping(target = "visitorPhone", source = "visitor.phoneNumber")
    @Mapping(target = "visitorCompany", source = "visitorCompany")
    VisitResponseDto toResponseDto(Visit visit);

    VisitResponseDto.CompanionResponseDto toCompanionResponseDto(Companion companion);

    @Mapping(target = "visitId", source = "id")
    @Mapping(target = "visitorName", source = "visitor.name")
    VisitSummaryResponseDto toVisitSummaryResponseDto(Visit visit);

    List<VisitSummaryResponseDto> toVisitSummaryResponseDtoList(List<Visit> visits);

    @Mapping(target = "visitId", source = "id")
    @Mapping(target = "visitorName", source = "visitor.name")
    @Mapping(target = "hostDepartment", source = "user.department.name")
    @Mapping(target = "phoneNumber", source = "visitor.phoneNumber")
    MyVisitResponseDto toMyVisitResponseDto(Visit visit);
}
