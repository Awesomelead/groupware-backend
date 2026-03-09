package kr.co.awesomelead.groupware_backend.domain.payslip.mapper;

import kr.co.awesomelead.groupware_backend.domain.payslip.dto.response.AdminPayslipDetailDto;
import kr.co.awesomelead.groupware_backend.domain.payslip.dto.response.AdminPayslipSummaryDto;
import kr.co.awesomelead.groupware_backend.domain.payslip.dto.response.EmployeePayslipDetailDto;
import kr.co.awesomelead.groupware_backend.domain.payslip.dto.response.EmployeePayslipSummaryDto;
import kr.co.awesomelead.groupware_backend.domain.payslip.entity.Payslip;
import kr.co.awesomelead.groupware_backend.global.infra.s3.service.S3Service;

import org.mapstruct.Context;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

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
    @Mapping(
            target = "presignedUrl",
            expression = "java(s3Service.getPresignedViewUrl(payslip.getFileKey()))")
    AdminPayslipDetailDto toAdminPayslipDetailDto(Payslip payslip, @Context S3Service s3Service);

    @Mapping(target = "payslipId", source = "payslip.id")
    EmployeePayslipSummaryDto toEmployeePayslipSummaryDto(Payslip payslip);

    List<EmployeePayslipSummaryDto> toEmployeePayslipSummaryDtoList(List<Payslip> payslips);

    @Mapping(
            target = "presignedUrl",
            expression = "java(s3Service.getPresignedViewUrl(payslip.getFileKey()))")
    @Mapping(target = "payslipId", source = "payslip.id")
    EmployeePayslipDetailDto toEmployeePayslipDetailDto(
            Payslip payslip, @Context S3Service s3Service);
}
