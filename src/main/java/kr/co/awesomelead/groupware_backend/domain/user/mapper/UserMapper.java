package kr.co.awesomelead.groupware_backend.domain.user.mapper;

import kr.co.awesomelead.groupware_backend.domain.auth.dto.request.JoinRequestDto;
import kr.co.awesomelead.groupware_backend.domain.user.entity.User;
import kr.co.awesomelead.groupware_backend.domain.user.enums.Role;
import kr.co.awesomelead.groupware_backend.domain.user.enums.Status;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(
        componentModel = "spring",
        imports = {Role.class, Status.class})
public interface UserMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "password", ignore = true)
    @Mapping(target = "workLocation", source = "company")
    @Mapping(target = "birthDate", ignore = true)
    @Mapping(target = "role", expression = "java(Role.USER)")
    @Mapping(target = "status", expression = "java(Status.PENDING)")

    // 관리자만 설정 가능한 필드들
    @Mapping(target = "hireDate", ignore = true)
    @Mapping(target = "resignationDate", ignore = true)
    @Mapping(target = "jobType", ignore = true)
    @Mapping(target = "position", ignore = true)

    // 연관관계 필드들
    @Mapping(target = "annualLeave", ignore = true)
    @Mapping(target = "visits", ignore = true)
    @Mapping(target = "checkSheets", ignore = true)
    @Mapping(target = "leaveRequests", ignore = true)
    @Mapping(target = "payslips", ignore = true)
    @Mapping(target = "sentMessages", ignore = true)
    @Mapping(target = "receivedMessages", ignore = true)
    @Mapping(target = "department", ignore = true)
    @Mapping(target = "authorities", ignore = true)
    User toEntity(JoinRequestDto dto);
}
