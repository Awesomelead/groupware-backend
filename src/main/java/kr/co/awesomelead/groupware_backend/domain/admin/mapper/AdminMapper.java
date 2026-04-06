package kr.co.awesomelead.groupware_backend.domain.admin.mapper;

import kr.co.awesomelead.groupware_backend.domain.admin.dto.response.AdminPendingMyInfoDetailResponseDto;
import kr.co.awesomelead.groupware_backend.domain.user.dto.response.MyInfoAuthorityItemDto;
import kr.co.awesomelead.groupware_backend.domain.user.entity.MyInfoUpdateRequest;
import kr.co.awesomelead.groupware_backend.domain.user.enums.Authority;

import org.mapstruct.AfterMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

import java.util.Arrays;
import java.util.List;

@Mapper(componentModel = "spring")
public interface AdminMapper {

    @Mapping(target = "requestId", source = "id")
    @Mapping(target = "requestedAt", source = "createdAt")
    @Mapping(target = "userId", source = "user.id")
    @Mapping(target = "nameKor", source = "user.nameKor")
    @Mapping(target = "nameEng", source = "user.nameEng")
    @Mapping(target = "email", source = "user.email")
    @Mapping(target = "birthDate", source = "user.birthDate")
    @Mapping(target = "nationality", source = "user.nationality")
    @Mapping(target = "zipcode", source = "user.zipcode")
    @Mapping(target = "address1", source = "user.address1")
    @Mapping(target = "address2", source = "user.address2")
    @Mapping(target = "registrationNumber", source = "user.registrationNumber")
    @Mapping(target = "phoneNumber", source = "user.phoneNumber")
    @Mapping(target = "workLocation", source = "user.workLocation")
    @Mapping(target = "departmentId", source = "user.department.id")
    @Mapping(target = "departmentName", source = "user.department.name")
    @Mapping(target = "position", source = "user.position")
    @Mapping(target = "jobType", source = "user.jobType")
    @Mapping(target = "hireDate", source = "user.hireDate")
    @Mapping(target = "resignationDate", source = "user.resignationDate")
    @Mapping(target = "role", source = "user.role")
    @Mapping(target = "signupStatus", source = "user.status")
    @Mapping(target = "hasPendingMyInfoRequest", constant = "true")
    @Mapping(target = "authorities", ignore = true)
    AdminPendingMyInfoDetailResponseDto toDetailDto(MyInfoUpdateRequest request);

    @AfterMapping
    default void setAuthorities(
            MyInfoUpdateRequest request,
            @MappingTarget AdminPendingMyInfoDetailResponseDto.AdminPendingMyInfoDetailResponseDtoBuilder<?, ?> builder) {
        if (request.getUser() == null) {
            return;
        }
        List<MyInfoAuthorityItemDto> authorities =
                Arrays.stream(Authority.values())
                        .map(
                                a ->
                                        MyInfoAuthorityItemDto.builder()
                                                .code(a.name())
                                                .label(a.getDescription())
                                                .enabled(
                                                        request.getUser().getAuthorities() != null
                                                                && request.getUser()
                                                                        .getAuthorities()
                                                                        .contains(a))
                                                .build())
                        .toList();
        builder.authorities(authorities);
    }
}
