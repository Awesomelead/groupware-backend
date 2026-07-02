package kr.co.awesomelead.groupware_backend.domain.visit.mapper;

import kr.co.awesomelead.groupware_backend.domain.department.enums.Company;
import kr.co.awesomelead.groupware_backend.domain.visit.dto.request.LongTermVisitRequestDto;
import kr.co.awesomelead.groupware_backend.domain.visit.dto.request.MyVisitUpdateRequestDto;
import kr.co.awesomelead.groupware_backend.domain.visit.dto.request.OnSiteVisitRequestDto;
import kr.co.awesomelead.groupware_backend.domain.visit.dto.request.OneDayVisitRequestDto;
import kr.co.awesomelead.groupware_backend.domain.visit.dto.response.MyVisitDetailResponseDto;
import kr.co.awesomelead.groupware_backend.domain.visit.dto.response.MyVisitListResponseDto;
import kr.co.awesomelead.groupware_backend.domain.visit.dto.response.VisitDetailResponseDto;
import kr.co.awesomelead.groupware_backend.domain.visit.dto.response.VisitHostResponseDto;
import kr.co.awesomelead.groupware_backend.domain.visit.dto.response.VisitListResponseDto;
import kr.co.awesomelead.groupware_backend.domain.visit.dto.response.VisitRecordResponseDto;
import kr.co.awesomelead.groupware_backend.domain.visit.entity.Visit;
import kr.co.awesomelead.groupware_backend.domain.visit.entity.VisitHost;
import kr.co.awesomelead.groupware_backend.domain.visit.entity.VisitRecord;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.mapstruct.ReportingPolicy;

import java.time.LocalDateTime;
import java.util.List;

@Mapper(
        componentModel = "spring",
        unmappedTargetPolicy = ReportingPolicy.IGNORE,
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface VisitMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "visitorName", source = "dto.visitorName")
    @Mapping(target = "visitorPhoneNumber", source = "dto.visitorPhoneNumber")
    @Mapping(target = "visitorCompany", source = "dto.visitorCompany")
    @Mapping(target = "hostCompany", source = "hostCompany")
    @Mapping(target = "carNumber", source = "dto.carNumber")
    @Mapping(target = "hosts", ignore = true)
    @Mapping(target = "password", source = "encodedPassword")
    @Mapping(target = "startDate", source = "dto.visitDate")
    @Mapping(target = "endDate", source = "dto.visitDate")
    @Mapping(target = "plannedEntryTime", source = "dto.plannedEntryTime")
    @Mapping(target = "plannedExitTime", source = "dto.plannedExitTime")
    @Mapping(
            target = "status",
            expression =
                    "java(kr.co.awesomelead.groupware_backend.domain.visit.enums.VisitStatus.NOT_VISITED)")
    @Mapping(
            target = "visitCategory",
            expression =
                    "java(kr.co.awesomelead.groupware_backend.domain.visit.enums.VisitCategory.PRE_ONE_DAY)")
    @Mapping(target = "visited", constant = "false")
    @Mapping(target = "records", ignore = true)
    @Mapping(target = "phoneNumberHash", ignore = true)
    @Mapping(target = "permissionType", ignore = true)
    @Mapping(target = "permissionDetail", ignore = true)
    @Mapping(target = "purpose", ignore = true)
    Visit toOneDayVisit(OneDayVisitRequestDto dto, Company hostCompany, String encodedPassword);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "hosts", ignore = true)
    @Mapping(target = "password", source = "encodedPassword")
    @Mapping(target = "hostCompany", source = "hostCompany")
    @Mapping(target = "plannedEntryTime", ignore = true)
    @Mapping(target = "plannedExitTime", ignore = true)
    @Mapping(
            target = "status",
            expression =
                    "java(kr.co.awesomelead.groupware_backend.domain.visit.enums.VisitStatus.PENDING)")
    @Mapping(
            target = "visitCategory",
            expression =
                    "java(kr.co.awesomelead.groupware_backend.domain.visit.enums.VisitCategory.PRE_LONG_TERM)")
    @Mapping(target = "records", ignore = true)
    @Mapping(target = "phoneNumberHash", ignore = true)
    @Mapping(target = "visited", constant = "false")
    @Mapping(target = "purpose", ignore = true)
    Visit toLongTermVisit(LongTermVisitRequestDto dto, Company hostCompany, String encodedPassword);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "hosts", ignore = true)
    @Mapping(target = "password", source = "encodedPassword")
    @Mapping(
            target = "visitCategory",
            expression =
                    "java(kr.co.awesomelead.groupware_backend.domain.visit.enums.VisitCategory.ON_SITE)")
    @Mapping(
            target = "status",
            expression =
                    "java(kr.co.awesomelead.groupware_backend.domain.visit.enums.VisitStatus.IN_PROGRESS)")
    @Mapping(target = "visited", constant = "true")
    @Mapping(target = "startDate", expression = "java(java.time.LocalDate.now())")
    @Mapping(target = "endDate", expression = "java(java.time.LocalDate.now())")
    @Mapping(target = "plannedEntryTime", expression = "java(java.time.LocalTime.now())")
    @Mapping(target = "plannedExitTime", ignore = true)
    @Mapping(target = "hostCompany", source = "hostCompany")
    @Mapping(target = "records", ignore = true)
    @Mapping(target = "phoneNumberHash", ignore = true)
    @Mapping(target = "purpose", ignore = true)
    @Mapping(target = "permissionType", ignore = true)
    @Mapping(target = "permissionDetail", ignore = true)
    Visit toOnSiteVisit(OnSiteVisitRequestDto dto, Company hostCompany, String encodedPassword);

    @Mapping(target = "userId", source = "user.id")
    @Mapping(target = "name", expression = "java(visitHost.getUser().getDisplayName())")
    @Mapping(target = "position", source = "user.position")
    @Mapping(target = "departmentName", source = "user.department.name")
    VisitHostResponseDto toVisitHostResponseDto(VisitHost visitHost);

    List<VisitHostResponseDto> toVisitHostResponseDtoList(List<VisitHost> visitHosts);

    @Mapping(target = "visitId", source = "id")
    @Mapping(target = "purpose", source = "purpose")
    MyVisitListResponseDto toMyVisitListResponseDto(Visit visit);

    List<MyVisitListResponseDto> toMyVisitListResponseDtoList(List<Visit> visits);

    @Mapping(target = "visitId", source = "id")
    @Mapping(target = "hosts", source = "hosts")
    @Mapping(target = "entryTime", expression = "java(getEntryTimeLogic(visit))")
    @Mapping(target = "exitTime", expression = "java(getExitTimeLogic(visit))")
    @Mapping(target = "records", source = "records")
    @Mapping(target = "rejectionReason", source = "rejectionReason")
    MyVisitDetailResponseDto toMyVisitDetailResponseDto(Visit visit);

    default LocalDateTime getEntryTimeLogic(Visit visit) {
        if (visit.getRecords() != null && !visit.getRecords().isEmpty()) {
            if (visit.getRecords().get(0).getEntryTime() != null) {
                return visit.getRecords().get(0).getEntryTime();
            }
        }
        if (visit.getPlannedEntryTime() == null) {
            return null;
        }
        return LocalDateTime.of(visit.getStartDate(), visit.getPlannedEntryTime());
    }

    default LocalDateTime getExitTimeLogic(Visit visit) {
        if (visit.getRecords() != null && !visit.getRecords().isEmpty()) {
            if (visit.getRecords().get(0).getExitTime() != null) {
                return visit.getRecords().get(0).getExitTime();
            }
        }
        if (visit.getPlannedExitTime() == null) {
            return null;
        }
        return LocalDateTime.of(visit.getStartDate(), visit.getPlannedExitTime());
    }

    @Mapping(target = "hosts", source = "hosts")
    VisitListResponseDto toVisitListResponseDto(Visit visit);

    @Mapping(target = "hosts", source = "hosts")
    @Mapping(target = "records", source = "records")
    VisitDetailResponseDto toVisitDetailResponseDto(Visit visit);

    @Mapping(target = "signatureUrl", source = "signatureKey")
    VisitRecordResponseDto toRecordResponseDto(VisitRecord record);

    List<VisitListResponseDto> toVisitListResponseDtos(List<Visit> visits);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "password", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "hosts", ignore = true)
    @Mapping(target = "visitorPhoneNumber", ignore = true)
    @Mapping(target = "phoneNumberHash", ignore = true)
    @Mapping(target = "startDate", source = "startDate")
    @Mapping(target = "endDate", source = "endDate")
    void updateVisitFromDto(MyVisitUpdateRequestDto dto, @MappingTarget Visit visit);
}
