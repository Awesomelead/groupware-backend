package kr.co.awesomelead.groupware_backend.domain.payslip.mapper;

import java.util.List;
import kr.co.awesomelead.groupware_backend.domain.payslip.dto.response.AdminPayslipDetailDto;
import kr.co.awesomelead.groupware_backend.domain.payslip.dto.response.AdminPayslipSummaryDto;
import kr.co.awesomelead.groupware_backend.domain.payslip.dto.response.EmployeePayslipDetailDto;
import kr.co.awesomelead.groupware_backend.domain.payslip.dto.response.EmployeePayslipSummaryDto;
import kr.co.awesomelead.groupware_backend.domain.payslip.entity.Payslip;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface PayslipMapper {

    @Mapping(target = "payslipId", source = "payslip.id")
    @Mapping(target = "employeeName", source = "user.displayName")
    @Mapping(target = "employPosition", source = "user.position")
    AdminPayslipSummaryDto toAdminPayslipSummaryDto(Payslip payslip);

    List<AdminPayslipSummaryDto> toAdminPayslipSummaryDtoList(List<Payslip> payslips);

    @Mapping(target = "payslipId", source = "payslip.id")
    @Mapping(target = "employeeName", source = "user.displayName")
    @Mapping(target = "employPosition", source = "user.position")
    AdminPayslipDetailDto toAdminPayslipDetailDto(Payslip payslip);

    @Mapping(target = "payslipId", source = "payslip.id")
    EmployeePayslipSummaryDto toEmployeePayslipSummaryDto(Payslip payslip);

    List<EmployeePayslipSummaryDto> toEmployeePayslipSummaryDtoList(List<Payslip> payslips);

    EmployeePayslipDetailDto toEmployeePayslipDetailDto(Payslip payslip);

}
