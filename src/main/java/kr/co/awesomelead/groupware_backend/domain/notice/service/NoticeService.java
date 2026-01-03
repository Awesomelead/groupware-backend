package kr.co.awesomelead.groupware_backend.domain.notice.service;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import kr.co.awesomelead.groupware_backend.domain.notice.dto.request.NoticeCreateRequestDto;
import kr.co.awesomelead.groupware_backend.domain.notice.dto.request.NoticeUpdateRequestDto;
import kr.co.awesomelead.groupware_backend.domain.notice.dto.response.NoticeDetailDto;
import kr.co.awesomelead.groupware_backend.domain.notice.dto.response.NoticeSummaryDto;
import kr.co.awesomelead.groupware_backend.domain.notice.entity.Notice;
import kr.co.awesomelead.groupware_backend.domain.notice.entity.NoticeAttachment;
import kr.co.awesomelead.groupware_backend.domain.notice.enums.NoticeType;
import kr.co.awesomelead.groupware_backend.domain.notice.mapper.NoticeMapper;
import kr.co.awesomelead.groupware_backend.domain.notice.respository.NoticeAttachmentRepository;
import kr.co.awesomelead.groupware_backend.domain.notice.respository.NoticeRepository;
import kr.co.awesomelead.groupware_backend.domain.user.entity.User;
import kr.co.awesomelead.groupware_backend.domain.user.enums.Authority;
import kr.co.awesomelead.groupware_backend.domain.user.repository.UserRepository;
import kr.co.awesomelead.groupware_backend.global.CustomException;
import kr.co.awesomelead.groupware_backend.global.ErrorCode;
import kr.co.awesomelead.groupware_backend.global.infra.s3.S3Service;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
public class NoticeService {

    private final NoticeRepository noticeRepository;
    private final NoticeAttachmentRepository noticeAttachmentRepository;
    private final NoticeMapper noticeMapper;
    private final S3Service s3Service;
    private final UserRepository userRepository;

    private User validateAndGetAuthor(Long userId) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        if (!user.hasAuthority(Authority.WRITE_NOTICE)) {
            throw new CustomException(ErrorCode.NO_AUTHORITY_FOR_NOTICE);
        }
        return user;
    }

    @Transactional
    public Long createNotice(NoticeCreateRequestDto requestDto, List<MultipartFile> files,
        Long userId) throws IOException {
        User author = validateAndGetAuthor(userId);

        Notice notice = noticeMapper.toNoticeEntity(requestDto);
        notice.setAuthor(author);
        notice.setCreatedDate(LocalDateTime.now());
        notice.setUpdatedDate(LocalDateTime.now());

        uploadFiles(files, notice);

        return noticeRepository.save(notice).getId();
    }

    @Transactional(readOnly = true)
    public List<NoticeSummaryDto> getNoticesByType(NoticeType type) {
        List<Notice> notices = (type == null)
            ? noticeRepository.findAllByOrderByPinnedDescUpdatedDateDesc() // 전체 정렬 조회
            : noticeRepository.findByTypeOrderByPinnedDescUpdatedDateDesc(type); // 타입별 정렬 조회

        return noticeMapper.toNoticeSummaryDtoList(notices);
    }

    @Transactional(readOnly = true)
    public NoticeDetailDto getNotice(Long noticeId) {
        Notice notice = noticeRepository.findByIdWithDetails(noticeId)
            .orElseThrow(() -> new CustomException(ErrorCode.NOTICE_NOT_FOUND));

        // 조회수 증가
        notice.increaseViewCount();
        noticeRepository.save(notice);

        return noticeMapper.toNoticeDetailDto(notice, s3Service);
    }

    @Transactional
    public void deleteNotice(Long userId, Long noticeId) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
        if (!user.hasAuthority(Authority.WRITE_NOTICE)) {
            throw new CustomException(ErrorCode.NO_AUTHORITY_FOR_NOTICE);
        }

        Notice notice = noticeRepository.findByIdWithDetails(noticeId)
            .orElseThrow(() -> new CustomException(ErrorCode.NOTICE_NOT_FOUND));

        // 첨부파일 S3에서 삭제
        for (NoticeAttachment attachment : notice.getAttachments()) {
            s3Service.deleteFile(attachment.getS3Key());
        }

        // 공지사항 삭제 (첨부파일도 함께 삭제됨 - CascadeType.ALL)
        noticeRepository.delete(notice);
    }

    @Transactional
    public Long updateNotice(Long userId, Long noticeId, NoticeUpdateRequestDto dto,
        List<MultipartFile> newFiles)
        throws IOException {
        validateAndGetAuthor(userId);
        Notice notice = noticeRepository.findByIdWithDetails(noticeId)
            .orElseThrow(() -> new CustomException(ErrorCode.NOTICE_NOT_FOUND));

        notice.update(
            dto.getTitle(),
            dto.getContent(),
            dto.getPinned()
        );

        if (dto.getAttachmentsIdsToRemove() != null) {
            for (Long attachmentId : dto.getAttachmentsIdsToRemove()) {
                NoticeAttachment attachment = noticeAttachmentRepository.findById(attachmentId)
                    .orElseThrow(() -> new CustomException(ErrorCode.NOTICE_ATTACHMENT_NOT_FOUND));
                // S3에서 파일 삭제
                s3Service.deleteFile(attachment.getS3Key());
                // DB에서 첨부파일 삭제
                notice.removeAttachment(attachment);
                noticeAttachmentRepository.delete(attachment);
            }
        }

        uploadFiles(newFiles, notice);

        return noticeId;
    }

    private void uploadFiles(List<MultipartFile> files, Notice notice) throws IOException {
        if (files != null && !files.isEmpty()) {
            for (MultipartFile file : files) {
                String s3Key = s3Service.uploadFile(file);
                NoticeAttachment attachment = new NoticeAttachment();
                attachment.setOriginalFileName(file.getOriginalFilename());
                attachment.setS3Key(s3Key);
                attachment.setFileSize(file.getSize());
                notice.addAttachment(attachment);
            }
        }
    }
}