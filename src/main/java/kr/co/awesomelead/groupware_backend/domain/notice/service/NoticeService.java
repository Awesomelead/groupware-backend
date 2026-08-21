package kr.co.awesomelead.groupware_backend.domain.notice.service;

import kr.co.awesomelead.groupware_backend.domain.department.dto.response.UserSummaryResponseDto;
import kr.co.awesomelead.groupware_backend.domain.department.entity.Department;
import kr.co.awesomelead.groupware_backend.domain.department.enums.Company;
import kr.co.awesomelead.groupware_backend.domain.department.service.DepartmentService;
import kr.co.awesomelead.groupware_backend.domain.notice.dto.NoticeCompanyJobTypeTargetDto;
import kr.co.awesomelead.groupware_backend.domain.notice.dto.request.NoticeCreateRequestDto;
import kr.co.awesomelead.groupware_backend.domain.notice.dto.request.NoticeSearchConditionDto;
import kr.co.awesomelead.groupware_backend.domain.notice.dto.request.NoticeUpdateRequestDto;
import kr.co.awesomelead.groupware_backend.domain.notice.dto.response.NoticeDetailDto;
import kr.co.awesomelead.groupware_backend.domain.notice.dto.response.NoticeSummaryDto;
import kr.co.awesomelead.groupware_backend.domain.notice.entity.Notice;
import kr.co.awesomelead.groupware_backend.domain.notice.entity.NoticeAttachment;
import kr.co.awesomelead.groupware_backend.domain.notice.entity.NoticeTarget;
import kr.co.awesomelead.groupware_backend.domain.notice.mapper.NoticeMapper;
import kr.co.awesomelead.groupware_backend.domain.notice.respository.NoticeAttachmentRepository;
import kr.co.awesomelead.groupware_backend.domain.notice.respository.NoticeQueryRepository;
import kr.co.awesomelead.groupware_backend.domain.notice.respository.NoticeRepository;
import kr.co.awesomelead.groupware_backend.domain.notice.respository.NoticeTargetRepository;
import kr.co.awesomelead.groupware_backend.domain.notification.service.NotificationService;
import kr.co.awesomelead.groupware_backend.domain.user.entity.User;
import kr.co.awesomelead.groupware_backend.domain.user.enums.Authority;
import kr.co.awesomelead.groupware_backend.domain.user.enums.Role;
import kr.co.awesomelead.groupware_backend.domain.user.repository.UserRepository;
import kr.co.awesomelead.groupware_backend.global.error.CustomException;
import kr.co.awesomelead.groupware_backend.global.error.ErrorCode;
import kr.co.awesomelead.groupware_backend.global.infra.s3.service.S3Service;

import lombok.RequiredArgsConstructor;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class NoticeService {

    private final NoticeRepository noticeRepository;
    private final NoticeQueryRepository noticeQueryRepository;
    private final NoticeAttachmentRepository noticeAttachmentRepository;
    private final NoticeTargetRepository noticeTargetRepository;
    private final NoticeMapper noticeMapper;
    private final S3Service s3Service;
    private final UserRepository userRepository;
    private final DepartmentService departmentService;
    private final NotificationService notificationService;

    private User validateAndGetAuthor(Long userId) {
        User user =
                userRepository
                        .findById(userId)
                        .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        if (!user.hasAuthority(Authority.ACCESS_NOTICE)) {
            throw new CustomException(ErrorCode.NO_AUTHORITY_FOR_NOTICE);
        }
        return user;
    }

    @Transactional
    public Long createNotice(
            NoticeCreateRequestDto requestDto, List<MultipartFile> files, Long userId)
            throws IOException {
        User author = validateAndGetAuthor(userId);

        List<Long> targetUserIds = excludeMasterAdminTargetIds(requestDto.getTargetUserIds());

        Notice notice = noticeMapper.toNoticeEntity(requestDto, author);
        notice.update(
                null,
                requestDto.getTitle(),
                requestDto.getContent(),
                resolveSearchableText(requestDto.getContent()),
                null,
                requestDto.getTargetCompanies(),
                requestDto.getTargetCompanyJobTypes(),
                requestDto.getTargetDepartmentIds(),
                targetUserIds);
        noticeRepository.save(notice);

        Set<Long> finalTargetUserIds =
                resolveTargetUserIds(
                        requestDto.getTargetCompanies(),
                        requestDto.getTargetCompanyJobTypes(),
                        requestDto.getTargetDepartmentIds(),
                        targetUserIds);
        validateTargetsNotEmpty(finalTargetUserIds);

        List<NoticeTarget> targets =
                finalTargetUserIds.stream()
                        .map(
                                targetId ->
                                        NoticeTarget.builder()
                                                .notice(notice)
                                                .user(userRepository.getReferenceById(targetId))
                                                .build())
                        .toList();

        noticeTargetRepository.saveAll(targets);

        // 공지 대상자에게 알림 전송
        notificationService.sendNoticeAlertToTargets(
                notice.getTitle(), notice.getId(), finalTargetUserIds);

        uploadFiles(files, notice);

        return notice.getId();
    }

    @Transactional(readOnly = true)
    public Page<NoticeSummaryDto> getNoticesByType(
            NoticeSearchConditionDto conditionDto, Long userId, Pageable pageable) {
        User user =
                userRepository
                        .findById(userId)
                        .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
        boolean hasAccessNotice = user.hasAuthority(Authority.VIEW_ALL_NOTICE);

        return noticeQueryRepository.findNoticesWithFilters(
                conditionDto, userId, hasAccessNotice, pageable);
    }

    @Transactional
    public NoticeDetailDto getNotice(Long noticeId, Long userId) {
        Notice notice =
                noticeRepository
                        .findByIdWithDetails(noticeId)
                        .orElseThrow(() -> new CustomException(ErrorCode.NOTICE_NOT_FOUND));

        User user =
                userRepository
                        .findById(userId)
                        .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
        boolean hasAccessNotice = user.hasAuthority(Authority.VIEW_ALL_NOTICE);
        if (!canReadNotice(notice, userId, hasAccessNotice)) {
            throw new CustomException(ErrorCode.NO_AUTHORITY_FOR_NOTICE_READ);
        }

        // 조회수 증가
        notice.increaseViewCount();
        noticeRepository.save(notice);

        NoticeDetailDto dto = noticeMapper.toNoticeDetailDto(notice, s3Service);
        dto.setTargetUsers(toTargetUserResponses(noticeId));
        dto.setPrevNotice(
                noticeQueryRepository.findPrevNotice(
                        noticeId,
                        notice.getCreatedDate(),
                        notice.getType(),
                        userId,
                        hasAccessNotice));
        dto.setNextNotice(
                noticeQueryRepository.findNextNotice(
                        noticeId,
                        notice.getCreatedDate(),
                        notice.getType(),
                        userId,
                        hasAccessNotice));

        return dto;
    }

    private List<NoticeDetailDto.TargetUserResponse> toTargetUserResponses(Long noticeId) {
        return noticeTargetRepository.findAllByNoticeIdWithUserAndDepartment(noticeId).stream()
                .map(this::toTargetUserResponse)
                .toList();
    }

    private NoticeDetailDto.TargetUserResponse toTargetUserResponse(NoticeTarget target) {
        User user = target.getUser();
        Department department = user != null ? user.getDepartment() : null;

        String name = user != null ? user.getDisplayName() : null;
        String departmentName =
                department != null && department.getName() != null
                        ? department.getName().getDescription()
                        : null;
        String position =
                user != null && user.getPosition() != null
                        ? user.getPosition().getDescription()
                        : null;

        return NoticeDetailDto.TargetUserResponse.builder()
                .userId(user != null ? user.getId() : null)
                .name(name)
                .departmentId(department != null ? department.getId() : null)
                .departmentName(departmentName)
                .position(position)
                .jobType(user != null ? user.getJobType() : null)
                .targetName(toTargetUserName(departmentName, name, position))
                .build();
    }

    private String toTargetUserName(String departmentName, String name, String position) {
        if (!StringUtils.hasText(name)) {
            return null;
        }

        StringBuilder builder = new StringBuilder();
        if (StringUtils.hasText(departmentName)) {
            builder.append("[").append(departmentName).append("] ");
        }
        builder.append(name);
        if (StringUtils.hasText(position)) {
            builder.append(" (").append(position).append(")");
        }
        return builder.toString();
    }

    @Transactional
    public void deleteNotice(Long userId, Long noticeId) {
        validateAndGetAuthor(userId);

        Notice notice =
                noticeRepository
                        .findByIdWithDetails(noticeId)
                        .orElseThrow(() -> new CustomException(ErrorCode.NOTICE_NOT_FOUND));

        // 첨부파일 S3에서 삭제
        for (NoticeAttachment attachment : notice.getAttachments()) {
            s3Service.deleteFile(attachment.getS3Key());
        }

        // FK 자식 먼저 삭제
        noticeTargetRepository.deleteByNoticeId(noticeId);

        // 공지사항 삭제 (첨부파일도 함께 삭제됨 - CascadeType.ALL)
        noticeRepository.delete(notice);
    }

    @Transactional
    public Long updateNotice(
            Long userId, Long noticeId, NoticeUpdateRequestDto dto, List<MultipartFile> newFiles)
            throws IOException {
        validateAndGetAuthor(userId);
        Notice notice =
                noticeRepository
                        .findByIdWithDetails(noticeId)
                        .orElseThrow(() -> new CustomException(ErrorCode.NOTICE_NOT_FOUND));

        List<Long> targetUserIds = excludeMasterAdminTargetIds(dto.getTargetUserIds());

        notice.update(
                dto.getType(),
                dto.getTitle(),
                dto.getContent(),
                dto.getContent() != null ? resolveSearchableText(dto.getContent()) : null,
                dto.getPinned(),
                dto.getTargetCompanies(),
                dto.getTargetCompanyJobTypes(),
                dto.getTargetDepartmentIds(),
                targetUserIds);

        boolean shouldRebuildTargets =
                dto.getTargetCompanies() != null
                        || dto.getTargetCompanyJobTypes() != null
                        || dto.getTargetDepartmentIds() != null
                        || dto.getTargetUserIds() != null;

        if (shouldRebuildTargets) {
            List<Company> effectiveCompanies =
                    dto.getTargetCompanies() != null
                            ? dto.getTargetCompanies()
                            : notice.getTargetCompanies();
            List<NoticeCompanyJobTypeTargetDto> effectiveCompanyJobTypes =
                    dto.getTargetCompanyJobTypes() != null
                            ? dto.getTargetCompanyJobTypes()
                            : notice.getTargetCompanyJobTypes();
            List<Long> effectiveDepartmentIds =
                    dto.getTargetDepartmentIds() != null
                            ? dto.getTargetDepartmentIds()
                            : notice.getTargetDepartments();
            List<Long> effectiveUserIds =
                    targetUserIds != null
                            ? targetUserIds
                            : excludeMasterAdminTargetIds(notice.getTargetUsers());
            if (targetUserIds == null && effectiveUserIds != null) {
                notice.update(
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        effectiveUserIds);
            }

            Set<Long> finalTargetUserIds =
                    resolveTargetUserIds(
                            effectiveCompanies,
                            effectiveCompanyJobTypes,
                            effectiveDepartmentIds,
                            effectiveUserIds);

            validateTargetsNotEmpty(finalTargetUserIds);

            noticeTargetRepository.deleteByNoticeId(noticeId);

            if (!finalTargetUserIds.isEmpty()) {
                List<NoticeTarget> targets =
                        finalTargetUserIds.stream()
                                .map(
                                        targetId ->
                                                NoticeTarget.builder()
                                                        .notice(notice)
                                                        .user(
                                                                userRepository.getReferenceById(
                                                                        targetId))
                                                        .build())
                                .toList();
                noticeTargetRepository.saveAll(targets);
            }
        }

        if (dto.getAttachmentsIdsToRemove() != null) {
            for (Long attachmentId : dto.getAttachmentsIdsToRemove()) {
                NoticeAttachment attachment =
                        noticeAttachmentRepository
                                .findByIdAndNoticeId(attachmentId, noticeId)
                                .orElseThrow(
                                        () ->
                                                new CustomException(
                                                        ErrorCode.NOTICE_ATTACHMENT_NOT_FOUND));
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

    private Set<Long> resolveTargetUserIds(
            List<Company> targetCompanies,
            List<NoticeCompanyJobTypeTargetDto> targetCompanyJobTypes,
            List<Long> targetDepartmentIds,
            List<Long> targetUserIds) {
        Set<Long> finalTargetUserIds = new HashSet<>();

        if (targetCompanies != null) {
            for (Company company : targetCompanies) {
                List<Long> companyUserIds = userRepository.findAllIdsByCompany(company);
                finalTargetUserIds.addAll(companyUserIds);
            }
        }

        if (targetCompanyJobTypes != null) {
            for (NoticeCompanyJobTypeTargetDto target : targetCompanyJobTypes) {
                if (target.getCompany() == null || target.getJobType() == null) {
                    continue;
                }
                List<Long> companyJobTypeUserIds =
                        userRepository.findAllIdsByCompanyAndJobType(
                                target.getCompany(), target.getJobType());
                finalTargetUserIds.addAll(companyJobTypeUserIds);
            }
        }

        if (targetDepartmentIds != null) {
            for (Long deptId : targetDepartmentIds) {
                List<UserSummaryResponseDto> deptUsers =
                        departmentService.getUsersByDepartmentHierarchy(deptId);
                deptUsers.forEach(u -> finalTargetUserIds.add(u.getId()));
            }
        }

        if (targetUserIds != null) {
            finalTargetUserIds.addAll(targetUserIds);
        }

        excludeMasterAdminTargets(finalTargetUserIds);
        return finalTargetUserIds;
    }

    private boolean canReadNotice(Notice notice, Long userId, boolean hasAccessNotice) {
        if (hasAccessNotice) {
            return true;
        }
        if (notice.getAuthor() != null && userId.equals(notice.getAuthor().getId())) {
            return true;
        }
        return noticeTargetRepository.existsByNoticeIdAndUserId(notice.getId(), userId);
    }

    private void excludeMasterAdminTargets(Set<Long> targetUserIds) {
        targetUserIds.removeAll(findMasterAdminIds());
    }

    private List<Long> excludeMasterAdminTargetIds(List<Long> targetUserIds) {
        if (targetUserIds == null || targetUserIds.isEmpty()) {
            return targetUserIds;
        }

        Set<Long> masterAdminIds = findMasterAdminIds();
        return targetUserIds.stream().filter(userId -> !masterAdminIds.contains(userId)).toList();
    }

    private Set<Long> findMasterAdminIds() {
        List<User> masterAdmins = userRepository.findAllByRole(Role.MASTER_ADMIN);
        if (masterAdmins == null || masterAdmins.isEmpty()) {
            return Set.of();
        }
        return masterAdmins.stream().map(User::getId).collect(java.util.stream.Collectors.toSet());
    }

    private void validateTargetsNotEmpty(Set<Long> finalTargetUserIds) {
        if (finalTargetUserIds == null || finalTargetUserIds.isEmpty()) {
            throw new CustomException(ErrorCode.NOTICE_TARGET_REQUIRED);
        }
    }

    private String resolveSearchableText(String content) {
        String contentText = extractPlainTextFromHtml(content);
        if (StringUtils.hasText(contentText)) {
            return contentText;
        }

        if (StringUtils.hasText(content)) {
            return content.trim();
        }

        return "";
    }

    private String extractPlainTextFromHtml(String html) {
        if (!StringUtils.hasText(html)) {
            return "";
        }
        String text =
                html.replaceAll("(?is)<script[^>]*>.*?</script>", " ")
                        .replaceAll("(?is)<style[^>]*>.*?</style>", " ")
                        .replaceAll("(?is)<br\\s*/?>", "\n")
                        .replaceAll("(?is)</p>", "\n")
                        .replaceAll("(?is)</tr>", "\n")
                        .replaceAll("(?is)<[^>]+>", " ")
                        .replace("&nbsp;", " ")
                        .replace("&amp;", "&")
                        .replace("&lt;", "<")
                        .replace("&gt;", ">");
        return normalizeWhitespace(text);
    }

    private String normalizeWhitespace(String text) {
        if (!StringUtils.hasText(text)) {
            return "";
        }
        return text.replace('\u00A0', ' ')
                .replaceAll("[\\t\\x0B\\f\\r ]+", " ")
                .replaceAll(" *\\n *", "\n")
                .replaceAll("\\n{3,}", "\n\n")
                .trim();
    }

    private void uploadFiles(List<MultipartFile> files, Notice notice) throws IOException {
        if (files != null && !files.isEmpty()) {
            for (MultipartFile file : files) {
                if (file == null || file.isEmpty()) {
                    continue;
                }

                String originalFileName = file.getOriginalFilename();
                if (originalFileName == null || originalFileName.isBlank()) {
                    continue;
                }

                if ("blob".equalsIgnoreCase(originalFileName.trim())) {
                    continue;
                }

                String s3Key = s3Service.uploadFile(file);
                NoticeAttachment attachment = new NoticeAttachment();
                attachment.setOriginalFileName(originalFileName);
                attachment.setS3Key(s3Key);
                attachment.setFileSize(file.getSize());
                notice.addAttachment(attachment);
                noticeAttachmentRepository.save(attachment);
            }
        }
    }

    @Transactional(readOnly = true)
    public List<NoticeSummaryDto> getTop3NoticesForHome(Long userId) {
        User user =
                userRepository
                        .findById(userId)
                        .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        boolean hasAccessNotice = user.hasAuthority(Authority.VIEW_ALL_NOTICE);

        return noticeQueryRepository.findTop3Notices(userId, hasAccessNotice);
    }
}
