package kr.co.awesomelead.groupware_backend.domain.notice.mapper;

import kr.co.awesomelead.groupware_backend.domain.notice.dto.request.NoticeCreateRequestDto;
import kr.co.awesomelead.groupware_backend.domain.notice.dto.response.NoticeDetailDto;
import kr.co.awesomelead.groupware_backend.domain.notice.dto.response.NoticeSummaryDto;
import kr.co.awesomelead.groupware_backend.domain.notice.entity.Notice;
import kr.co.awesomelead.groupware_backend.domain.notice.entity.NoticeAttachment;
import kr.co.awesomelead.groupware_backend.global.infra.s3.S3Service;

import org.mapstruct.Context;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface NoticeMapper {

    Notice toNoticeEntity(NoticeCreateRequestDto dto);

    NoticeSummaryDto toNoticeSummaryDto(Notice notice);

    List<NoticeSummaryDto> toNoticeSummaryDtoList(List<Notice> notices);

    @Mapping(target = "authorName", expression = "java(notice.getAuthor().getDisplayName())")
    @Mapping(target = "attachments", source = "attachments")
    NoticeDetailDto toNoticeDetailDto(Notice notice, @Context S3Service s3Service);

    @Mapping(
            target = "viewUrl",
            expression = "java(s3Service.getPresignedViewUrl(attachment.getS3Key()))")
    NoticeDetailDto.AttachmentResponse toAttachmentResponse(
            NoticeAttachment attachment, @Context S3Service s3Service);
}
