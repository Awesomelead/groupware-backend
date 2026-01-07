package kr.co.awesomelead.groupware_backend.domain.annualleave.mapper;

import kr.co.awesomelead.groupware_backend.domain.annualleave.dto.response.AnnualLeaveResponseDto;
import kr.co.awesomelead.groupware_backend.domain.annualleave.entity.AnnualLeave;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface AnnualLeaveMapper {

    AnnualLeaveResponseDto toAnnualLeaveResponseDto(AnnualLeave annualLeave);

}
