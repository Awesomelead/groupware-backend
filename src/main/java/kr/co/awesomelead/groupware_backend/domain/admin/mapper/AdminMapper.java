package kr.co.awesomelead.groupware_backend.domain.admin.mapper;

import kr.co.awesomelead.groupware_backend.domain.admin.dto.response.AdminPendingMyInfoDetailResponseDto;
import kr.co.awesomelead.groupware_backend.domain.user.entity.MyInfoUpdateRequest;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface AdminMapper {

    @Mapping(target = "requestId", source = "id")
    @Mapping(target = "userId", source = "user.id")
    @Mapping(target = "nameKor", source = "user.nameKor")
    @Mapping(target = "email", source = "user.email")
    @Mapping(target = "currentNameEng", source = "user.nameEng")
    @Mapping(target = "currentPhoneNumber", source = "user.phoneNumber")
    @Mapping(target = "currentZipcode", source = "user.zipcode")
    @Mapping(target = "currentAddress1", source = "user.address1")
    @Mapping(target = "currentAddress2", source = "user.address2")
    @Mapping(target = "requestedAt", source = "createdAt")
    AdminPendingMyInfoDetailResponseDto toDetailDto(MyInfoUpdateRequest request);
}
