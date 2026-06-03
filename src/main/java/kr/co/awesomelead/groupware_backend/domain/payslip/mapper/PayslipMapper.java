package kr.co.awesomelead.groupware_backend.domain.payslip.mapper;

import kr.co.awesomelead.groupware_backend.domain.payslip.dto.response.AdminPayslipDetailDto;
import kr.co.awesomelead.groupware_backend.domain.payslip.dto.response.AdminPayslipSummaryDto;
import kr.co.awesomelead.groupware_backend.domain.payslip.dto.response.EmployeePayslipDetailDto;
import kr.co.awesomelead.groupware_backend.domain.payslip.dto.response.EmployeePayslipSummaryDto;
import kr.co.awesomelead.groupware_backend.domain.payslip.entity.Payslip;
import kr.co.awesomelead.groupware_backend.domain.user.enums.Position;
import kr.co.awesomelead.groupware_backend.global.error.CustomException;
import kr.co.awesomelead.groupware_backend.global.error.ErrorCode;
import kr.co.awesomelead.groupware_backend.global.infra.s3.service.S3Service;

import org.mapstruct.Context;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.text.Normalizer;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Mapper(componentModel = "spring")
public interface PayslipMapper {

    Pattern PAYSLIP_MONTH_PATTERN =
            Pattern.compile(
                    "^급여명세서\\(근로기준1\\)_[^_]+_[^_]+_([0-9]{6})\\.pdf$", Pattern.CASE_INSENSITIVE);

    @Mapping(target = "payslipId", source = "payslip.id")
    @Mapping(target = "employeeName", source = "user.displayName")
    @Mapping(target = "employPosition", expression = "java(buildPositionDescription(payslip))")
    @Mapping(target = "payslipTitle", expression = "java(buildPayslipTitle(payslip))")
    AdminPayslipSummaryDto toAdminPayslipSummaryDto(Payslip payslip);

    List<AdminPayslipSummaryDto> toAdminPayslipSummaryDtoList(List<Payslip> payslips);

    @Mapping(target = "payslipId", source = "payslip.id")
    @Mapping(target = "employeeName", source = "user.displayName")
    @Mapping(target = "employPosition", expression = "java(buildPositionDescription(payslip))")
    @Mapping(target = "senderName", source = "sentBy.displayName")
    @Mapping(
            target = "senderPosition",
            expression = "java(buildSenderPositionDescription(payslip))")
    @Mapping(target = "payslipTitle", expression = "java(buildPayslipTitle(payslip))")
    @Mapping(
            target = "presignedUrl",
            expression = "java(s3Service.getPresignedViewUrl(payslip.getFileKey()))")
    AdminPayslipDetailDto toAdminPayslipDetailDto(Payslip payslip, @Context S3Service s3Service);

    @Mapping(target = "payslipId", source = "payslip.id")
    @Mapping(target = "payslipTitle", expression = "java(buildPayslipTitle(payslip))")
    EmployeePayslipSummaryDto toEmployeePayslipSummaryDto(Payslip payslip);

    List<EmployeePayslipSummaryDto> toEmployeePayslipSummaryDtoList(List<Payslip> payslips);

    @Mapping(
            target = "presignedUrl",
            expression = "java(s3Service.getPresignedViewUrl(payslip.getFileKey()))")
    @Mapping(target = "payslipId", source = "payslip.id")
    @Mapping(target = "payslipTitle", expression = "java(buildPayslipTitle(payslip))")
    EmployeePayslipDetailDto toEmployeePayslipDetailDto(
            Payslip payslip, @Context S3Service s3Service);

    default String buildPayslipTitle(Payslip payslip) {
        if (payslip == null) {
            throw new CustomException(ErrorCode.PAYSLIP_NOT_FOUND);
        }
        return buildPayslipTitleFromOriginalFileName(payslip.getOriginalFileName());
    }

    default String buildPositionDescription(Payslip payslip) {
        if (payslip == null || payslip.getUser() == null) {
            return null;
        }
        Position position = payslip.getUser().getPosition();
        if (position == null) {
            return null;
        }
        return position.getDescription();
    }

    default String buildSenderPositionDescription(Payslip payslip) {
        if (payslip == null || payslip.getSentBy() == null) {
            return null;
        }
        Position position = payslip.getSentBy().getPosition();
        if (position == null) {
            return null;
        }
        return position.getDescription();
    }

    private static String buildPayslipTitleFromOriginalFileName(String originalFileName) {
        if (originalFileName == null || originalFileName.isBlank()) {
            throw new CustomException(ErrorCode.INVALID_PAYSLIP_FILE_NAME_FORMAT);
        }

        String normalizedPath = originalFileName.replace("\\", "/");
        String baseFileName = normalizedPath.substring(normalizedPath.lastIndexOf('/') + 1);
        baseFileName = Normalizer.normalize(baseFileName, Normalizer.Form.NFC);

        Matcher matcher = PAYSLIP_MONTH_PATTERN.matcher(baseFileName);
        if (!matcher.matches()) {
            throw new CustomException(ErrorCode.INVALID_PAYSLIP_FILE_NAME_FORMAT);
        }

        String yearMonth = matcher.group(1);
        int year = Integer.parseInt(yearMonth.substring(0, 4));
        int month = Integer.parseInt(yearMonth.substring(4, 6));

        if (month < 1 || month > 12) {
            throw new CustomException(ErrorCode.INVALID_PAYSLIP_FILE_NAME_FORMAT);
        }

        return year + "년 " + month + "월";
    }
}
