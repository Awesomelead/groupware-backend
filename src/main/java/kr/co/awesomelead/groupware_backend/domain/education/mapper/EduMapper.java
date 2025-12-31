package kr.co.awesomelead.groupware_backend.domain.education.mapper;

import java.util.List;
import kr.co.awesomelead.groupware_backend.domain.department.entity.Department;
import kr.co.awesomelead.groupware_backend.domain.education.dto.request.EduReportRequestDto;
import kr.co.awesomelead.groupware_backend.domain.education.dto.response.EduReportAdminDetailDto;
import kr.co.awesomelead.groupware_backend.domain.education.dto.response.EduReportDetailDto;
import kr.co.awesomelead.groupware_backend.domain.education.entity.EduAttachment;
import kr.co.awesomelead.groupware_backend.domain.education.entity.EduAttendance;
import kr.co.awesomelead.groupware_backend.domain.education.entity.EduReport;
import kr.co.awesomelead.groupware_backend.domain.user.entity.User;
import kr.co.awesomelead.groupware_backend.global.infra.s3.S3Service;
import org.mapstruct.Context;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface EduMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "attachments", ignore = true)
    @Mapping(target = "department", source = "department")
    EduReport toEduReportEntity(EduReportRequestDto dto, Department department);

    @Mapping(target = "attendance", ignore = true)
    @Mapping(target = "attachments", source = "attachments")
    EduReportDetailDto toDetailDto(EduReport eduReport, @Context S3Service s3Service);

    @Mapping(target = "viewUrl", expression = "java(s3Service.getFileUrl(attachment.getS3Key()))")
    EduReportDetailDto.AttachmentResponse toAttachmentDto(EduAttachment attachment,
        @Context S3Service s3Service);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "user", source = "user")
    @Mapping(target = "eduReport", source = "report")
    @Mapping(target = "attendance", constant = "true") // 출석은 항상 true로 설정
    @Mapping(target = "signatureKey", source = "signatureKey")
    EduAttendance toEduAttendanceEntity(User user, EduReport report, String signatureKey);

    @Mapping(target = "id", source = "report.id")
    @Mapping(target = "title", source = "report.title")
    @Mapping(target = "eduType", source = "report.eduType")
    @Mapping(target = "content", source = "report.content")
    @Mapping(target = "eduDate", source = "report.eduDate")
    @Mapping(target = "numberOfPeople", source = "numberOfPeople")
    @Mapping(target = "numberOfAttendees", expression = "java(attendances.size())")
    @Mapping(target = "attendees", source = "attendances")
    EduReportAdminDetailDto toAdminDetailDto(
        EduReport report,
        List<EduAttendance> attendances,
        long numberOfPeople,
        @Context S3Service s3Service
    );

    @Mapping(target = "userName", expression = "java(attendance.getUser().getDisplayName())")
    @Mapping(target = "signatureUrl", expression = "java(attendance.getSignatureKey() != null ? s3Service.getFileUrl(attendance.getSignatureKey()) : null)")
    EduReportAdminDetailDto.AttendeeInfo toAttendeeInfo(
        EduAttendance attendance,
        @Context S3Service s3Service
    );

}
