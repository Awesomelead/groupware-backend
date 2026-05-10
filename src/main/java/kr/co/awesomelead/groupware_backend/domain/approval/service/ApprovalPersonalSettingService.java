package kr.co.awesomelead.groupware_backend.domain.approval.service;

import kr.co.awesomelead.groupware_backend.domain.approval.dto.request.ApprovalPersonalSettingUpdateRequestDto;
import kr.co.awesomelead.groupware_backend.domain.approval.dto.response.ApprovalPersonalSettingResponseDto;
import kr.co.awesomelead.groupware_backend.domain.approval.entity.ApprovalPersonalSetting;
import kr.co.awesomelead.groupware_backend.domain.approval.entity.ApprovalPersonalViewerTarget;
import kr.co.awesomelead.groupware_backend.domain.approval.enums.ApprovalTargetType;
import kr.co.awesomelead.groupware_backend.domain.approval.repository.ApprovalPersonalSettingRepository;
import kr.co.awesomelead.groupware_backend.domain.department.entity.Department;
import kr.co.awesomelead.groupware_backend.domain.department.repository.DepartmentRepository;
import kr.co.awesomelead.groupware_backend.domain.user.entity.User;
import kr.co.awesomelead.groupware_backend.domain.user.repository.UserRepository;
import kr.co.awesomelead.groupware_backend.global.error.CustomException;
import kr.co.awesomelead.groupware_backend.global.error.ErrorCode;
import kr.co.awesomelead.groupware_backend.global.infra.s3.service.S3Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class ApprovalPersonalSettingService {

    private static final long SIGNATURE_IMAGE_MAX_BYTES = 100 * 1024; // 100KB

    private final ApprovalPersonalSettingRepository approvalPersonalSettingRepository;
    private final UserRepository userRepository;
    private final DepartmentRepository departmentRepository;
    private final S3Service s3Service;

    @Transactional(readOnly = true)
    public ApprovalPersonalSettingResponseDto getMySetting(Long userId) {
        ApprovalPersonalSetting setting =
                approvalPersonalSettingRepository.findByUserIdWithTargets(userId).orElse(null);
        if (setting == null) {
            return ApprovalPersonalSettingResponseDto.builder()
                    .delegateEnabled(false)
                    .defaultViewerTargets(List.of())
                    .build();
        }
        return toResponse(setting);
    }

    @Transactional
    public ApprovalPersonalSettingResponseDto saveMySetting(
            Long userId, ApprovalPersonalSettingUpdateRequestDto request) {
        User me = getUser(userId);
        ApprovalPersonalSetting setting =
                approvalPersonalSettingRepository
                        .findByUserIdWithTargets(userId)
                        .orElseGet(() -> createDefaultSetting(me));

        applyDelegateSetting(setting, me, request);
        replaceDefaultViewerTargets(setting, request.getDefaultViewerTargets());

        ApprovalPersonalSetting saved = approvalPersonalSettingRepository.save(setting);
        return toResponse(saved);
    }

    @Transactional
    public String uploadSignatureImage(Long userId, MultipartFile signatureImage) {
        validateSignatureImage(signatureImage);

        User me = getUser(userId);
        ApprovalPersonalSetting setting =
                approvalPersonalSettingRepository
                        .findByUserIdWithTargets(userId)
                        .orElseGet(() -> createDefaultSetting(me));

        String oldSignatureKey = setting.getSignatureImageKey();
        String uploadedKey = null;
        try {
            uploadedKey = s3Service.uploadFile(signatureImage);
            setting.setSignatureImageKey(uploadedKey);
            approvalPersonalSettingRepository.save(setting);

            if (StringUtils.hasText(oldSignatureKey)) {
                safeDeleteFile(oldSignatureKey);
            }
            return s3Service.getPresignedViewUrl(uploadedKey);
        } catch (IOException e) {
            if (StringUtils.hasText(uploadedKey)) {
                safeDeleteFile(uploadedKey);
            }
            throw new CustomException(ErrorCode.FILE_UPLOAD_ERROR);
        }
    }

    @Transactional
    public void deleteSignatureImage(Long userId) {
        ApprovalPersonalSetting setting =
                approvalPersonalSettingRepository.findByUserId(userId).orElse(null);
        if (setting == null || !StringUtils.hasText(setting.getSignatureImageKey())) {
            return;
        }

        safeDeleteFile(setting.getSignatureImageKey());
        setting.setSignatureImageKey(null);
    }

    private ApprovalPersonalSetting createDefaultSetting(User user) {
        return ApprovalPersonalSetting.builder()
                .user(user)
                .delegateEnabled(false)
                .defaultViewerTargets(new ArrayList<>())
                .build();
    }

    private void applyDelegateSetting(
            ApprovalPersonalSetting setting,
            User me,
            ApprovalPersonalSettingUpdateRequestDto request) {
        boolean delegateEnabled = Boolean.TRUE.equals(request.getDelegateEnabled());
        setting.setDelegateEnabled(delegateEnabled);

        if (!delegateEnabled) {
            setting.setDelegateUser(null);
            setting.setDelegateStartDate(null);
            setting.setDelegateEndDate(null);
            return;
        }

        Long delegateUserId = request.getDelegateUserId();
        LocalDate delegateStartDate = request.getDelegateStartDate();
        LocalDate delegateEndDate = request.getDelegateEndDate();

        if (delegateUserId == null || delegateStartDate == null || delegateEndDate == null) {
            throw new CustomException(ErrorCode.INVALID_ARGUMENT);
        }
        if (delegateUserId.equals(me.getId())) {
            throw new CustomException(ErrorCode.INVALID_ARGUMENT);
        }
        if (delegateStartDate.isAfter(delegateEndDate)) {
            throw new CustomException(ErrorCode.INVALID_ARGUMENT);
        }

        User delegateUser = getUser(delegateUserId);
        setting.setDelegateUser(delegateUser);
        setting.setDelegateStartDate(delegateStartDate);
        setting.setDelegateEndDate(delegateEndDate);
    }

    private void replaceDefaultViewerTargets(
            ApprovalPersonalSetting setting,
            List<ApprovalPersonalSettingUpdateRequestDto.DefaultViewerTargetRequestDto> requests) {
        setting.getDefaultViewerTargets().clear();
        if (requests == null || requests.isEmpty()) {
            return;
        }

        Set<String> deduplicatedTargets = new LinkedHashSet<>();
        int sortOrder = 1;
        for (ApprovalPersonalSettingUpdateRequestDto.DefaultViewerTargetRequestDto request :
                requests) {
            validateDefaultViewerTargetRequest(request);

            ApprovalTargetType targetType = request.getTargetType();
            Long targetId =
                    targetType == ApprovalTargetType.USER
                            ? request.getTargetUserId()
                            : request.getTargetDepartmentId();
            String dedupeKey = targetType.name() + ":" + targetId;
            if (!deduplicatedTargets.add(dedupeKey)) {
                continue;
            }

            ApprovalPersonalViewerTarget target =
                    ApprovalPersonalViewerTarget.builder()
                            .setting(setting)
                            .targetType(targetType)
                            .sortOrder(sortOrder++)
                            .build();

            if (targetType == ApprovalTargetType.USER) {
                User targetUser = getUser(request.getTargetUserId());
                target.setTargetUser(targetUser);
                target.setTargetDepartment(null);
                target.setTargetNameSnapshot(toUserTargetName(targetUser));
            } else {
                Department targetDepartment = getDepartment(request.getTargetDepartmentId());
                target.setTargetDepartment(targetDepartment);
                target.setTargetUser(null);
                target.setTargetNameSnapshot(toDepartmentTargetName(targetDepartment));
            }
            setting.getDefaultViewerTargets().add(target);
        }
    }

    private void validateDefaultViewerTargetRequest(
            ApprovalPersonalSettingUpdateRequestDto.DefaultViewerTargetRequestDto request) {
        if (request == null || request.getTargetType() == null) {
            throw new CustomException(ErrorCode.INVALID_ARGUMENT);
        }

        if (request.getTargetType() == ApprovalTargetType.USER) {
            if (request.getTargetUserId() == null) {
                throw new CustomException(ErrorCode.INVALID_ARGUMENT);
            }
            return;
        }

        if (request.getTargetDepartmentId() == null) {
            throw new CustomException(ErrorCode.INVALID_ARGUMENT);
        }
    }

    private ApprovalPersonalSettingResponseDto toResponse(ApprovalPersonalSetting setting) {
        ApprovalPersonalSettingResponseDto.DelegateUserDto delegateUserDto = null;
        if (setting.getDelegateUser() != null) {
            delegateUserDto = toDelegateUserDto(setting.getDelegateUser());
        }

        List<ApprovalPersonalSettingResponseDto.DefaultViewerTargetDto> defaultViewerTargets =
                setting.getDefaultViewerTargets().stream()
                        .sorted(
                                Comparator.comparing(
                                                ApprovalPersonalViewerTarget::getSortOrder,
                                                Comparator.nullsLast(Integer::compareTo))
                                        .thenComparing(
                                                ApprovalPersonalViewerTarget::getId,
                                                Comparator.nullsLast(Long::compareTo)))
                        .map(this::toDefaultViewerTargetDto)
                        .toList();

        String signatureImageUrl =
                StringUtils.hasText(setting.getSignatureImageKey())
                        ? s3Service.getPresignedViewUrl(setting.getSignatureImageKey())
                        : null;

        return ApprovalPersonalSettingResponseDto.builder()
                .delegateEnabled(Boolean.TRUE.equals(setting.getDelegateEnabled()))
                .delegateUser(delegateUserDto)
                .delegateStartDate(setting.getDelegateStartDate())
                .delegateEndDate(setting.getDelegateEndDate())
                .signatureImageUrl(signatureImageUrl)
                .defaultViewerTargets(defaultViewerTargets)
                .build();
    }

    private ApprovalPersonalSettingResponseDto.DelegateUserDto toDelegateUserDto(User user) {
        return ApprovalPersonalSettingResponseDto.DelegateUserDto.builder()
                .id(user.getId())
                .name(user.getNameKor())
                .position(user.getPosition() != null ? user.getPosition().getDescription() : null)
                .departmentName(
                        user.getDepartment() != null
                                        && user.getDepartment().getName() != null
                                ? user.getDepartment().getName().getDescription()
                                : null)
                .build();
    }

    private ApprovalPersonalSettingResponseDto.DefaultViewerTargetDto toDefaultViewerTargetDto(
            ApprovalPersonalViewerTarget target) {
        User targetUser = target.getTargetUser();
        Department targetDepartment = target.getTargetDepartment();

        return ApprovalPersonalSettingResponseDto.DefaultViewerTargetDto.builder()
                .id(target.getId())
                .targetType(target.getTargetType())
                .targetUserId(targetUser != null ? targetUser.getId() : null)
                .targetUserName(targetUser != null ? targetUser.getNameKor() : null)
                .targetUserPosition(
                        targetUser != null && targetUser.getPosition() != null
                                ? targetUser.getPosition().getDescription()
                                : null)
                .targetUserDepartmentName(
                        targetUser != null
                                        && targetUser.getDepartment() != null
                                        && targetUser.getDepartment().getName() != null
                                ? targetUser.getDepartment().getName().getDescription()
                                : null)
                .targetDepartmentId(targetDepartment != null ? targetDepartment.getId() : null)
                .targetDepartmentName(
                        targetDepartment != null && targetDepartment.getName() != null
                                ? targetDepartment.getName().getDescription()
                                : null)
                .targetName(target.getTargetNameSnapshot())
                .sortOrder(target.getSortOrder())
                .build();
    }

    private void validateSignatureImage(MultipartFile signatureImage) {
        if (signatureImage == null || signatureImage.isEmpty()) {
            throw new CustomException(ErrorCode.INVALID_ARGUMENT);
        }

        if (signatureImage.getSize() > SIGNATURE_IMAGE_MAX_BYTES) {
            throw new CustomException(ErrorCode.INVALID_ARGUMENT);
        }

        String contentType =
                signatureImage.getContentType() != null
                        ? signatureImage.getContentType().toLowerCase(Locale.ROOT)
                        : "";
        String extension =
                StringUtils.getFilenameExtension(signatureImage.getOriginalFilename()) != null
                        ? StringUtils.getFilenameExtension(signatureImage.getOriginalFilename())
                                .toLowerCase(Locale.ROOT)
                        : "";

        boolean validContentType =
                Set.of("image/png", "image/jpg", "image/jpeg", "image/gif")
                        .contains(contentType);
        boolean validExtension = Set.of("png", "jpg", "jpeg", "gif").contains(extension);

        if (!validContentType && !validExtension) {
            throw new CustomException(ErrorCode.INVALID_ARGUMENT);
        }
    }

    private void safeDeleteFile(String fileKey) {
        try {
            s3Service.deleteFile(fileKey);
        } catch (Exception e) {
            log.warn("서명이미지 삭제 실패 - key: {}", fileKey, e);
        }
    }

    private String toUserTargetName(User user) {
        String departmentName =
                user.getDepartment() != null && user.getDepartment().getName() != null
                        ? user.getDepartment().getName().getDescription()
                        : "-";
        String positionName = user.getPosition() != null ? user.getPosition().getDescription() : "-";
        return "[" + departmentName + "] " + user.getNameKor() + " (" + positionName + ")";
    }

    private String toDepartmentTargetName(Department department) {
        String departmentName =
                department.getName() != null ? department.getName().getDescription() : "-";
        return "[" + departmentName + "]";
    }

    private User getUser(Long userId) {
        return userRepository
                .findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
    }

    private Department getDepartment(Long departmentId) {
        return departmentRepository
                .findById(departmentId)
                .orElseThrow(() -> new CustomException(ErrorCode.DEPARTMENT_NOT_FOUND));
    }
}
