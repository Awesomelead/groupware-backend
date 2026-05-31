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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Mapper(componentModel = "spring")
public interface PayslipMapper {

    Pattern PAYSLIP_MONTH_PATTERN =
            Pattern.compile(
                    "^급여명세서\\(근로기준1\\)_[^_]+_[^_]+_([0-9]{6})\\.pdf$",
                    Pattern.CASE_INSENSITIVE);

    @Mapping(target = "payslipId", source = "payslip.id")
    @Mapping(target = "employeeName", source = "user.displayName")
    @Mapping(target = "employPosition", source = "user.position")
    @Mapping(
            target = "payslipTitle",
            expression = "java(buildPayslipTitle(payslip.getOriginalFileName()))")
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
    @Mapping(
            target = "payslipTitle",
            expression = "java(buildPayslipTitle(payslip.getOriginalFileName()))")
    EmployeePayslipSummaryDto toEmployeePayslipSummaryDto(Payslip payslip);

    List<EmployeePayslipSummaryDto> toEmployeePayslipSummaryDtoList(List<Payslip> payslips);

    @Mapping(
            target = "presignedUrl",
            expression = "java(s3Service.getPresignedViewUrl(payslip.getFileKey()))")
    @Mapping(target = "payslipId", source = "payslip.id")
    EmployeePayslipDetailDto toEmployeePayslipDetailDto(
            Payslip payslip, @Context S3Service s3Service);

    default String buildPayslipTitle(String originalFileName) {
        if (originalFileName == null || originalFileName.isBlank()) {
            return "급여명세서";
        }

        String normalizedPath = originalFileName.replace("\\", "/");
        String baseFileName = normalizedPath.substring(normalizedPath.lastIndexOf('/') + 1);

        Matcher matcher = PAYSLIP_MONTH_PATTERN.matcher(baseFileName);
        if (!matcher.matches()) {
            return "급여명세서";
        }

        String yearMonth = matcher.group(1);
        int year = Integer.parseInt(yearMonth.substring(0, 4));
        int month = Integer.parseInt(yearMonth.substring(4, 6));

        if (month < 1 || month > 12) {
            return "급여명세서";
        }

        return year + "년 " + month + "월 급여명세서";
    }
}
