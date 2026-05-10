package kr.co.awesomelead.groupware_backend.domain.approval.service;

import kr.co.awesomelead.groupware_backend.domain.approval.dto.request.SavedApprovalLineDetailRequestDto;
import kr.co.awesomelead.groupware_backend.domain.approval.dto.request.SavedDepartmentApprovalLineUpsertRequestDto;
import kr.co.awesomelead.groupware_backend.domain.approval.dto.request.SavedPersonalApprovalLineUpsertRequestDto;
import kr.co.awesomelead.groupware_backend.domain.approval.dto.response.SavedApprovalLineResponseDto;
import kr.co.awesomelead.groupware_backend.domain.approval.entity.SavedApprovalLine;
import kr.co.awesomelead.groupware_backend.domain.approval.entity.SavedApprovalLineDetail;
import kr.co.awesomelead.groupware_backend.domain.approval.enums.ApprovalRouteRole;
import kr.co.awesomelead.groupware_backend.domain.approval.enums.ApprovalSavedLineType;
import kr.co.awesomelead.groupware_backend.domain.approval.enums.ApprovalTargetType;
import kr.co.awesomelead.groupware_backend.domain.approval.enums.ApprovalType;
import kr.co.awesomelead.groupware_backend.domain.approval.repository.SavedApprovalLineDetailRepository;
import kr.co.awesomelead.groupware_backend.domain.approval.repository.SavedApprovalLineRepository;
import kr.co.awesomelead.groupware_backend.domain.department.entity.Department;
import kr.co.awesomelead.groupware_backend.domain.department.repository.DepartmentRepository;
import kr.co.awesomelead.groupware_backend.domain.user.entity.User;
import kr.co.awesomelead.groupware_backend.domain.user.enums.Authority;
import kr.co.awesomelead.groupware_backend.domain.user.enums.Position;
import kr.co.awesomelead.groupware_backend.domain.user.enums.Role;
import kr.co.awesomelead.groupware_backend.domain.user.repository.UserRepository;
import kr.co.awesomelead.groupware_backend.global.error.CustomException;
import kr.co.awesomelead.groupware_backend.global.error.ErrorCode;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SavedApprovalLineService {

    private final SavedApprovalLineRepository savedApprovalLineRepository;
    private final SavedApprovalLineDetailRepository savedApprovalLineDetailRepository;
    private final UserRepository userRepository;
    private final DepartmentRepository departmentRepository;

    @Transactional(readOnly = true)
    public List<SavedApprovalLineResponseDto> getPersonalLines(Long userId) {
        List<SavedApprovalLine> lines =
                savedApprovalLineRepository.findAllPersonalWithDetails(
                        userId, ApprovalSavedLineType.PERSONAL);
        return lines.stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public SavedApprovalLineResponseDto getPersonalLine(Long userId, Long lineId) {
        SavedApprovalLine line = getSavedLine(lineId);
        if (line.getLineType() != ApprovalSavedLineType.PERSONAL
                || line.getOwnerUser() == null
                || !line.getOwnerUser().getId().equals(userId)) {
            throw new CustomException(ErrorCode.NO_AUTHORITY_FOR_SAVED_APPROVAL_LINE_READ);
        }
        return toResponse(line);
    }

    @Transactional
    public Long createPersonalLine(Long userId, SavedPersonalApprovalLineUpsertRequestDto request) {
        User owner = getUser(userId);
        boolean isDefaultRequested = isTrue(request.getIsDefault());

        if (isDefaultRequested) {
            savedApprovalLineRepository.clearPersonalDefault(
                    owner.getId(), ApprovalSavedLineType.PERSONAL, request.getApprovalType());
        }

        SavedApprovalLine line =
                SavedApprovalLine.builder()
                        .lineType(ApprovalSavedLineType.PERSONAL)
                        .lineName(request.getLineName().trim())
                        .approvalType(request.getApprovalType())
                        .ownerUser(owner)
                        .department(null)
                        .createdByUser(owner)
                        .isActive(true)
                        .isDefault(isDefaultRequested)
                        .build();
        SavedApprovalLine saved = savedApprovalLineRepository.save(line);
        replaceDetails(saved, request.getLines());
        return saved.getId();
    }

    @Transactional
    public void updatePersonalLine(
            Long userId, Long lineId, SavedPersonalApprovalLineUpsertRequestDto request) {
        SavedApprovalLine line = getSavedLine(lineId);
        if (line.getLineType() != ApprovalSavedLineType.PERSONAL
                || line.getOwnerUser() == null
                || !line.getOwnerUser().getId().equals(userId)) {
            throw new CustomException(ErrorCode.NO_AUTHORITY_FOR_SAVED_APPROVAL_LINE_READ);
        }
        boolean isDefaultRequested = isTrue(request.getIsDefault());
        if (isDefaultRequested) {
            savedApprovalLineRepository.clearPersonalDefaultExcept(
                    userId,
                    ApprovalSavedLineType.PERSONAL,
                    request.getApprovalType(),
                    line.getId());
        }
        line.setLineName(request.getLineName().trim());
        line.setApprovalType(request.getApprovalType());
        line.setIsDefault(isDefaultRequested);
        replaceDetails(line, request.getLines());
    }

    @Transactional
    public void deletePersonalLine(Long userId, Long lineId) {
        SavedApprovalLine line = getSavedLine(lineId);
        if (line.getLineType() != ApprovalSavedLineType.PERSONAL
                || line.getOwnerUser() == null
                || !line.getOwnerUser().getId().equals(userId)) {
            throw new CustomException(ErrorCode.NO_AUTHORITY_FOR_SAVED_APPROVAL_LINE_READ);
        }
        savedApprovalLineDetailRepository.deleteBySavedLineId(lineId);
        savedApprovalLineRepository.delete(line);
    }

    @Transactional(readOnly = true)
    public List<SavedApprovalLineResponseDto> getDepartmentLines(Long userId) {
        User user = getUser(userId);
        Department department = user.getDepartment();
        if (department == null) {
            throw new CustomException(ErrorCode.DEPARTMENT_NOT_FOUND);
        }

        List<SavedApprovalLine> lines =
                savedApprovalLineRepository.findAllDepartmentWithDetails(
                        department.getId(), ApprovalSavedLineType.DEPARTMENT);
        return lines.stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public SavedApprovalLineResponseDto getDepartmentLine(Long userId, Long lineId) {
        User user = getUser(userId);
        SavedApprovalLine line = getSavedLine(lineId);

        if (line.getLineType() != ApprovalSavedLineType.DEPARTMENT
                || line.getDepartment() == null) {
            throw new CustomException(ErrorCode.SAVED_APPROVAL_LINE_NOT_FOUND);
        }

        if (!canReadDepartmentLine(user, line)) {
            throw new CustomException(ErrorCode.NO_AUTHORITY_FOR_SAVED_APPROVAL_LINE_READ);
        }

        return toResponse(line);
    }

    @Transactional
    public Long createDepartmentLine(
            Long userId, SavedDepartmentApprovalLineUpsertRequestDto request) {
        User actor = getUser(userId);
        validateDepartmentLineManageAuthority(actor);

        Department department = resolveTargetDepartment(actor, request.getDepartmentId());
        boolean isDefaultRequested = isTrue(request.getIsDefault());

        if (isDefaultRequested) {
            savedApprovalLineRepository.clearDepartmentDefault(
                    department.getId(),
                    ApprovalSavedLineType.DEPARTMENT,
                    request.getApprovalType());
        }

        SavedApprovalLine line =
                SavedApprovalLine.builder()
                        .lineType(ApprovalSavedLineType.DEPARTMENT)
                        .lineName(request.getLineName().trim())
                        .approvalType(request.getApprovalType())
                        .ownerUser(null)
                        .department(department)
                        .createdByUser(actor)
                        .isActive(true)
                        .isDefault(isDefaultRequested)
                        .build();
        SavedApprovalLine saved = savedApprovalLineRepository.save(line);
        replaceDetails(saved, request.getLines());
        return saved.getId();
    }

    @Transactional
    public void updateDepartmentLine(
            Long userId, Long lineId, SavedDepartmentApprovalLineUpsertRequestDto request) {
        User actor = getUser(userId);
        validateDepartmentLineManageAuthority(actor);

        SavedApprovalLine line = getSavedLine(lineId);
        if (line.getLineType() != ApprovalSavedLineType.DEPARTMENT
                || line.getDepartment() == null) {
            throw new CustomException(ErrorCode.SAVED_APPROVAL_LINE_NOT_FOUND);
        }

        if (!canManageDepartmentLine(actor, line)) {
            throw new CustomException(ErrorCode.NO_AUTHORITY_FOR_DEPARTMENT_APPROVAL_LINE);
        }

        Department targetDepartment = resolveTargetDepartment(actor, request.getDepartmentId());
        boolean isDefaultRequested = isTrue(request.getIsDefault());
        if (isDefaultRequested) {
            savedApprovalLineRepository.clearDepartmentDefaultExcept(
                    targetDepartment.getId(),
                    ApprovalSavedLineType.DEPARTMENT,
                    request.getApprovalType(),
                    line.getId());
        }

        line.setLineName(request.getLineName().trim());
        line.setApprovalType(request.getApprovalType());
        line.setDepartment(targetDepartment);
        line.setIsDefault(isDefaultRequested);
        replaceDetails(line, request.getLines());
    }

    @Transactional
    public void deleteDepartmentLine(Long userId, Long lineId) {
        User actor = getUser(userId);
        validateDepartmentLineManageAuthority(actor);

        SavedApprovalLine line = getSavedLine(lineId);
        if (line.getLineType() != ApprovalSavedLineType.DEPARTMENT
                || line.getDepartment() == null) {
            throw new CustomException(ErrorCode.SAVED_APPROVAL_LINE_NOT_FOUND);
        }

        if (!canManageDepartmentLine(actor, line)) {
            throw new CustomException(ErrorCode.NO_AUTHORITY_FOR_DEPARTMENT_APPROVAL_LINE);
        }

        savedApprovalLineDetailRepository.deleteBySavedLineId(lineId);
        savedApprovalLineRepository.delete(line);
    }

    private void replaceDetails(
            SavedApprovalLine savedLine, List<SavedApprovalLineDetailRequestDto> detailRequests) {
        savedApprovalLineDetailRepository.deleteBySavedLineId(savedLine.getId());
        if (detailRequests == null || detailRequests.isEmpty()) {
            return;
        }

        List<SavedApprovalLineDetail> details = new ArrayList<>();
        for (int i = 0; i < detailRequests.size(); i++) {
            SavedApprovalLineDetailRequestDto request = detailRequests.get(i);
            validateDetailRequest(request);

            SavedApprovalLineDetail detail = new SavedApprovalLineDetail();
            detail.setSavedLine(savedLine);
            detail.setRole(request.getRole());
            detail.setTargetType(request.getTargetType());
            detail.setSequenceNo(request.getSequenceNo() != null ? request.getSequenceNo() : i + 1);
            detail.setIsRequired(request.getRequired());

            if (request.getTargetType() == ApprovalTargetType.USER) {
                User targetUser = getUser(request.getTargetUserId());
                detail.setTargetUser(targetUser);
                detail.setTargetDepartment(null);
                detail.setTargetNameSnapshot(
                        toTargetName(ApprovalTargetType.USER, targetUser, null));
            } else {
                Department targetDepartment = getDepartment(request.getTargetDepartmentId());
                detail.setTargetDepartment(targetDepartment);
                detail.setTargetUser(null);
                detail.setTargetNameSnapshot(
                        toTargetName(ApprovalTargetType.DEPARTMENT, null, targetDepartment));
            }

            details.add(detail);
        }

        savedApprovalLineDetailRepository.saveAll(details);
    }

    private void validateDetailRequest(SavedApprovalLineDetailRequestDto request) {
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

    private SavedApprovalLine getSavedLine(Long lineId) {
        return savedApprovalLineRepository
                .findWithDetailsById(lineId)
                .orElseThrow(() -> new CustomException(ErrorCode.SAVED_APPROVAL_LINE_NOT_FOUND));
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

    private boolean canReadDepartmentLine(User user, SavedApprovalLine line) {
        if (isGlobalDepartmentLineManager(user)) {
            return true;
        }
        return user.getDepartment() != null
                && line.getDepartment() != null
                && user.getDepartment().getId().equals(line.getDepartment().getId());
    }

    private boolean canManageDepartmentLine(User user, SavedApprovalLine line) {
        if (isGlobalDepartmentLineManager(user)) {
            return true;
        }
        return user.getDepartment() != null
                && line.getDepartment() != null
                && user.getDepartment().getId().equals(line.getDepartment().getId());
    }

    private Department resolveTargetDepartment(User actor, Long departmentId) {
        if (departmentId == null) {
            if (actor.getDepartment() == null) {
                throw new CustomException(ErrorCode.DEPARTMENT_NOT_FOUND);
            }
            return actor.getDepartment();
        }

        Department target = getDepartment(departmentId);
        if (!isGlobalDepartmentLineManager(actor)) {
            if (actor.getDepartment() == null
                    || !actor.getDepartment().getId().equals(target.getId())) {
                throw new CustomException(ErrorCode.NO_AUTHORITY_FOR_DEPARTMENT_APPROVAL_LINE);
            }
        }
        return target;
    }

    private void validateDepartmentLineManageAuthority(User user) {
        if (!user.hasAuthority(Authority.MANAGE_APPROVAL_LINE)) {
            throw new CustomException(ErrorCode.NO_AUTHORITY_FOR_DEPARTMENT_APPROVAL_LINE);
        }
    }

    private boolean isGlobalDepartmentLineManager(User user) {
        return user.getRole() == Role.MASTER_ADMIN || user.getPosition() == Position.CEO;
    }

    private SavedApprovalLineResponseDto toResponse(SavedApprovalLine line) {
        List<SavedApprovalLineResponseDto.LineDetailDto> details =
                line.getDetails().stream()
                        .sorted(
                                Comparator.comparing(
                                                SavedApprovalLineDetail::getSequenceNo,
                                                Comparator.nullsLast(Integer::compareTo))
                                        .thenComparing(SavedApprovalLineDetail::getId))
                        .map(this::toDetailResponse)
                        .toList();
        SavedApprovalLineResponseDto.ApprovalBoxPreviewDto approvalBoxPreview =
                buildApprovalBoxPreview(details);

        String ownerUserName =
                line.getOwnerUser() != null
                        ? (StringUtils.hasText(line.getOwnerUser().getNameKor())
                                ? line.getOwnerUser().getNameKor()
                                : line.getOwnerUser().getNameEng())
                        : null;
        String departmentName =
                line.getDepartment() != null
                        ? line.getDepartment().getName().getDescription()
                        : null;
        String createdByUserName =
                line.getCreatedByUser() != null
                        ? (StringUtils.hasText(line.getCreatedByUser().getNameKor())
                                ? line.getCreatedByUser().getNameKor()
                                : line.getCreatedByUser().getNameEng())
                        : null;

        return SavedApprovalLineResponseDto.builder()
                .id(line.getId())
                .lineType(line.getLineType())
                .lineTypeLabel(line.getLineType().getDescription())
                .lineName(line.getLineName())
                .approvalType(line.getApprovalType())
                .approvalTypeLabel(
                        line.getApprovalType() != null
                                ? line.getApprovalType().getDescription()
                                : null)
                .isDefault(isTrue(line.getIsDefault()))
                .ownerUserId(line.getOwnerUser() != null ? line.getOwnerUser().getId() : null)
                .ownerUserName(ownerUserName)
                .departmentId(line.getDepartment() != null ? line.getDepartment().getId() : null)
                .departmentName(departmentName)
                .createdByUserId(
                        line.getCreatedByUser() != null ? line.getCreatedByUser().getId() : null)
                .createdByUserName(createdByUserName)
                .createdAt(line.getCreatedAt())
                .modifiedAt(line.getModifiedAt())
                .lines(details)
                .approvalBoxPreview(approvalBoxPreview)
                .build();
    }

    private SavedApprovalLineResponseDto.LineDetailDto toDetailResponse(
            SavedApprovalLineDetail detail) {
        User targetUser = detail.getTargetUser();
        Department targetDepartment = detail.getTargetDepartment();

        String targetUserName =
                targetUser == null
                        ? null
                        : (StringUtils.hasText(targetUser.getNameKor())
                                ? targetUser.getNameKor()
                                : targetUser.getNameEng());
        String targetUserPosition =
                targetUser != null && targetUser.getPosition() != null
                        ? targetUser.getPosition().getDescription()
                        : null;
        String targetUserDepartmentName =
                targetUser != null && targetUser.getDepartment() != null
                        ? targetUser.getDepartment().getName().getDescription()
                        : null;
        String targetDepartmentName =
                targetDepartment != null ? targetDepartment.getName().getDescription() : null;

        return SavedApprovalLineResponseDto.LineDetailDto.builder()
                .id(detail.getId())
                .role(detail.getRole())
                .targetType(detail.getTargetType())
                .targetUserId(targetUser != null ? targetUser.getId() : null)
                .targetUserName(targetUserName)
                .targetUserPosition(targetUserPosition)
                .targetUserDepartmentName(targetUserDepartmentName)
                .targetDepartmentId(targetDepartment != null ? targetDepartment.getId() : null)
                .targetDepartmentName(targetDepartmentName)
                .targetName(detail.getTargetNameSnapshot())
                .sequenceNo(detail.getSequenceNo())
                .required(detail.getIsRequired())
                .build();
    }

    private String toTargetName(
            ApprovalTargetType targetType, User targetUser, Department targetDepartment) {
        if (targetType == ApprovalTargetType.DEPARTMENT) {
            return targetDepartment != null
                    ? "[" + targetDepartment.getName().getDescription() + "]"
                    : null;
        }

        if (targetUser == null) {
            return null;
        }

        String departmentName =
                targetUser.getDepartment() != null
                        ? targetUser.getDepartment().getName().getDescription()
                        : "소속없음";
        String name =
                StringUtils.hasText(targetUser.getNameKor())
                        ? targetUser.getNameKor()
                        : targetUser.getNameEng();
        String position =
                targetUser.getPosition() != null ? targetUser.getPosition().getDescription() : null;

        if (StringUtils.hasText(position)) {
            return "[" + departmentName + "] " + name + " (" + position + ")";
        }
        return "[" + departmentName + "] " + name;
    }

    private SavedApprovalLineResponseDto.ApprovalBoxPreviewDto buildApprovalBoxPreview(
            List<SavedApprovalLineResponseDto.LineDetailDto> details) {
        List<SavedApprovalLineResponseDto.ApprovalBoxSlotDto> slots =
                details.stream()
                        .filter(detail -> detail.getRole() == ApprovalRouteRole.APPROVAL_LINE)
                        .sorted(
                                Comparator.comparing(
                                                SavedApprovalLineResponseDto.LineDetailDto::getSequenceNo,
                                                Comparator.nullsLast(Integer::compareTo))
                                        .thenComparing(
                                                SavedApprovalLineResponseDto.LineDetailDto::getId,
                                                Comparator.nullsLast(Long::compareTo)))
                        .map(
                                detail ->
                                        SavedApprovalLineResponseDto.ApprovalBoxSlotDto.builder()
                                                .sequenceNo(detail.getSequenceNo())
                                                .targetType(detail.getTargetType())
                                                .targetUserId(detail.getTargetUserId())
                                                .targetUserName(detail.getTargetUserName())
                                                .targetUserPosition(detail.getTargetUserPosition())
                                                .targetUserDepartmentName(
                                                        detail.getTargetUserDepartmentName())
                                                .targetDepartmentId(detail.getTargetDepartmentId())
                                                .targetDepartmentName(
                                                        detail.getTargetDepartmentName())
                                                .targetName(detail.getTargetName())
                                                .build())
                        .toList();
        return SavedApprovalLineResponseDto.ApprovalBoxPreviewDto.builder().slots(slots).build();
    }

    private boolean isTrue(Boolean value) {
        return Boolean.TRUE.equals(value);
    }
}
