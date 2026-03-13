package kr.co.awesomelead.groupware_backend.domain.education.mapper;

import kr.co.awesomelead.groupware_backend.domain.department.entity.Department;
import kr.co.awesomelead.groupware_backend.domain.education.dto.request.EduReportRequestDto;
import kr.co.awesomelead.groupware_backend.domain.education.dto.response.EduReportDetailDto;
import kr.co.awesomelead.groupware_backend.domain.education.entity.EduAttachment;
import kr.co.awesomelead.groupware_backend.domain.education.entity.EduAttendance;
import kr.co.awesomelead.groupware_backend.domain.education.entity.EduReport;
import kr.co.awesomelead.groupware_backend.domain.user.entity.User;
import kr.co.awesomelead.groupware_backend.global.infra.s3.service.S3Service;

import org.mapstruct.Context;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface EduMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "eduDate", ignore = true)
    @Mapping(target = "attachments", ignore = true)
    @Mapping(target = "department", source = "department")
    EduReport toEduReportEntity(EduReportRequestDto dto, Department department);

    /**
     * EduReport → EduReportDetailDto 변환
     *
     * @param attendances 출석자 목록 (ACCESS_EDUCATION 권한 없으면 null)
     * @param numberOfPeople 교육 대상 인원 수 (-1L이면 null로 처리)
     */
    @Mapping(target = "attendance", ignore = true)
    @Mapping(target = "attachments", source = "report.attachments")
    @Mapping(target = "eduType", source = "report.eduType")
    @Mapping(
            target = "departmentName",
            expression =
                    "java(report.getDepartment() != null"
                            + " ? report.getDepartment().getName().getDescription() : null)")
    @Mapping(
            target = "numberOfPeople",
            expression = "java(numberOfPeople >= 0 ? (int) numberOfPeople : null)")
    @Mapping(
            target = "numberOfAttendees",
            expression = "java(attendances != null ? attendances.size() : null)")
    @Mapping(target = "attendees", source = "attendances")
    EduReportDetailDto toDetailDto(
            EduReport report,
            List<EduAttendance> attendances,
            long numberOfPeople,
            @Context S3Service s3Service);

    @Mapping(
            target = "viewUrl",
            expression = "java(s3Service.getPresignedViewUrl(attachment.getS3Key()))")
    EduReportDetailDto.AttachmentResponse toAttachmentDto(
            EduAttachment attachment, @Context S3Service s3Service);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "user", source = "user")
    @Mapping(target = "eduReport", source = "report")
    @Mapping(target = "attendance", constant = "true")
    @Mapping(target = "signatureKey", source = "signatureKey")
    EduAttendance toEduAttendanceEntity(User user, EduReport report, String signatureKey);

    @Mapping(target = "userName", expression = "java(attendance.getUser().getDisplayName())")
    @Mapping(
            target = "signatureUrl",
            expression =
                    "java(attendance.getSignatureKey() != null ?"
                        + " s3Service.getPresignedViewUrl(attendance.getSignatureKey()) : null)")
    EduReportDetailDto.AttendeeInfo toAttendeeInfo(
            EduAttendance attendance, @Context S3Service s3Service);
}
