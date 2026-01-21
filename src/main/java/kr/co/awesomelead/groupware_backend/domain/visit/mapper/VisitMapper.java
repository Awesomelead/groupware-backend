package kr.co.awesomelead.groupware_backend.domain.visit.mapper;

import java.time.LocalDateTime;
import java.util.List;
import kr.co.awesomelead.groupware_backend.domain.user.entity.User;
import kr.co.awesomelead.groupware_backend.domain.visit.dto.request.LongTermVisitRequestDto;
import kr.co.awesomelead.groupware_backend.domain.visit.dto.request.OnSiteVisitRequestDto;
import kr.co.awesomelead.groupware_backend.domain.visit.dto.request.OneDayVisitRequestDto;
import kr.co.awesomelead.groupware_backend.domain.visit.dto.response.MyVisitDetailResponseDto;
import kr.co.awesomelead.groupware_backend.domain.visit.dto.response.MyVisitListResponseDto;
import kr.co.awesomelead.groupware_backend.domain.visit.dto.response.VisitDetailResponseDto;
import kr.co.awesomelead.groupware_backend.domain.visit.dto.response.VisitListResponseDto;
import kr.co.awesomelead.groupware_backend.domain.visit.dto.response.VisitRecordResponseDto;
import kr.co.awesomelead.groupware_backend.domain.visit.entity.Visit;
import kr.co.awesomelead.groupware_backend.domain.visit.entity.VisitRecord;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.WARN)
public interface VisitMapper {

    @Mapping(target = "id", ignore = true) // 생성 시 ID는 자동 생성되므로 무시
    @Mapping(target = "visitorName", source = "dto.visitorName")
    @Mapping(target = "visitorPhoneNumber", source = "dto.visitorPhoneNumber")
    @Mapping(target = "visitorCompany", source = "dto.visitorCompany")
    @Mapping(target = "hostCompany", source = "host.workLocation")
    @Mapping(target = "carNumber", source = "dto.carNumber")
    @Mapping(target = "user", source = "host") // 파라미터로 받은 User 객체를 매핑
    @Mapping(target = "password", source = "encodedPassword") // 암호화된 비밀번호 매핑
    @Mapping(target = "startDate", source = "dto.visitDate") // 시작일 = 방문일
    @Mapping(target = "endDate", source = "dto.visitDate")   // 종료일 = 방문일
    @Mapping(target = "plannedEntryTime", source = "dto.entryTime") // 추가
    @Mapping(target = "plannedExitTime", source = "dto.exitTime")   // 추가
    @Mapping(target = "status", expression = "java(kr.co.awesomelead.groupware_backend.domain.visit.enums.VisitStatus.NOT_VISITED)")
    @Mapping(target = "visitType", expression = "java(kr.co.awesomelead.groupware_backend.domain.visit.enums.VisitType.PRE_REGISTRATION)")
    @Mapping(target = "isLongTerm", constant = "false")
    @Mapping(target = "visited", constant = "false")
    @Mapping(target = "records", ignore = true)
    @Mapping(target = "phoneNumberHash", ignore = true)
    @Mapping(target = "permissionType", ignore = true)
    @Mapping(target = "permissionDetail", ignore = true)
    @Mapping(target = "purpose", ignore = true)
    Visit toOneDayVisit(OneDayVisitRequestDto dto, User host, String encodedPassword);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "user", source = "host")
    @Mapping(target = "password", source = "encodedPassword")
    @Mapping(target = "isLongTerm", constant = "true") // 장기 방문이므로 true
    @Mapping(target = "hostCompany", source = "host.workLocation")

    // 장기 방문은 예정 시간이 없으므로 매핑에서 제외 (null로 들어감)
    @Mapping(target = "plannedEntryTime", ignore = true)
    @Mapping(target = "plannedExitTime", ignore = true)

    @Mapping(target = "status", expression = "java(kr.co.awesomelead.groupware_backend.domain.visit.enums.VisitStatus.PENDING)")
    // 장기는 보통 '승인 대기'
    @Mapping(target = "visitType", expression = "java(kr.co.awesomelead.groupware_backend.domain.visit.enums.VisitType.PRE_REGISTRATION)")
    @Mapping(target = "records", ignore = true)
    @Mapping(target = "phoneNumberHash", ignore = true)
    @Mapping(target = "visited", constant = "false")
    @Mapping(target = "purpose", ignore = true)
    Visit toLongTermVisit(LongTermVisitRequestDto dto, User host, String encodedPassword);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "user", source = "host")
    @Mapping(target = "password", source = "encodedPassword")
    @Mapping(target = "visitType", expression = "java(kr.co.awesomelead.groupware_backend.domain.visit.enums.VisitType.ON_SITE)")
    @Mapping(target = "status", constant = "COMPLETED")
    @Mapping(target = "visited", constant = "true") // 즉시 방문 처리
    @Mapping(target = "isLongTerm", constant = "false")
    @Mapping(target = "startDate", expression = "java(java.time.LocalDate.now())")
    @Mapping(target = "endDate", expression = "java(java.time.LocalDate.now())")
    @Mapping(target = "plannedEntryTime", expression = "java(java.time.LocalTime.now())")
    @Mapping(target = "plannedExitTime", ignore = true)
    @Mapping(target = "hostCompany", source = "host.workLocation")
    @Mapping(target = "records", ignore = true)
    @Mapping(target = "phoneNumberHash", ignore = true)
    @Mapping(target = "purpose", ignore = true)
    @Mapping(target = "permissionType", ignore = true)
    @Mapping(target = "permissionDetail", ignore = true)
    Visit toOnSiteVisit(OnSiteVisitRequestDto dto, User host, String encodedPassword);


    // MyVisitListResponseDto 매핑
    @Mapping(target = "visitId", source = "id")
    MyVisitListResponseDto toMyVisitListResponseDto(Visit visit);

    List<MyVisitListResponseDto> toMyVisitListResponseDtoList(List<Visit> visits);

    // MyVisitDetailResponseDto 매핑
    @Mapping(target = "visitId", source = "id")
    @Mapping(target = "departmentName", source = "user.department.name")
    @Mapping(target = "entryTime", expression = "java(getEntryTimeLogic(visit))")
    @Mapping(target = "exitTime", expression = "java(getExitTimeLogic(visit))")
    MyVisitDetailResponseDto toMyVisitDetailResponseDto(Visit visit);

    default LocalDateTime getEntryTimeLogic(Visit visit) {
        if (!visit.getRecords().isEmpty() && visit.getRecords().get(0).getEntryTime() != null) {
            return visit.getRecords().get(0).getEntryTime();
        }
        return LocalDateTime.of(visit.getStartDate(), visit.getPlannedEntryTime());
    }

    default LocalDateTime getExitTimeLogic(Visit visit) {
        if (!visit.getRecords().isEmpty() && visit.getRecords().get(0).getExitTime() != null) {
            return visit.getRecords().get(0).getExitTime();
        }
        return LocalDateTime.of(visit.getStartDate(), visit.getPlannedExitTime());
    }

    @Mapping(target = "hostDepartmentName", source = "user.department.name")
    VisitListResponseDto toVisitListResponseDto(Visit visit);

    @Mapping(target = "hostDepartmentName", source = "user.department.name")
    @Mapping(target = "hostName", source = "user.nameKor")
    @Mapping(target = "records", source = "records")
        // VisitRecord -> VisitRecordResponseDto 변환 필요
    VisitDetailResponseDto toVisitDetailResponseDto(Visit visit);

    @Mapping(target = "signatureUrl", source = "signatureKey")
        // 키값을 URL로 변환하는 로직은 서비스나 커스텀 매퍼에서 처리 가능
    VisitRecordResponseDto toRecordResponseDto(VisitRecord record);

    List<VisitListResponseDto> toVisitListResponseDtos(List<Visit> visits);
}
