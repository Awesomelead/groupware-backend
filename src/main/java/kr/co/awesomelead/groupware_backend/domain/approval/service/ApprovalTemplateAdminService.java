package kr.co.awesomelead.groupware_backend.domain.approval.service;

import kr.co.awesomelead.groupware_backend.domain.approval.dto.request.ApprovalTemplateCategoryCreateRequestDto;
import kr.co.awesomelead.groupware_backend.domain.approval.dto.request.ApprovalTemplateCategoryUpdateRequestDto;
import kr.co.awesomelead.groupware_backend.domain.approval.dto.request.ApprovalTemplateCreateRequestDto;
import kr.co.awesomelead.groupware_backend.domain.approval.dto.request.ApprovalTemplateLineUpsertRequestDto;
import kr.co.awesomelead.groupware_backend.domain.approval.dto.request.ApprovalTemplateUpdateRequestDto;
import kr.co.awesomelead.groupware_backend.domain.approval.dto.response.ApprovalTemplateAdminResponseDto;
import kr.co.awesomelead.groupware_backend.domain.approval.dto.response.ApprovalTemplateCategoryResponseDto;
import kr.co.awesomelead.groupware_backend.domain.approval.entity.ApprovalTemplate;
import kr.co.awesomelead.groupware_backend.domain.approval.entity.ApprovalTemplateCategory;
import kr.co.awesomelead.groupware_backend.domain.approval.entity.ApprovalTemplateLine;
import kr.co.awesomelead.groupware_backend.domain.approval.enums.ApprovalLinePolicy;
import kr.co.awesomelead.groupware_backend.domain.approval.enums.ApprovalRouteRole;
import kr.co.awesomelead.groupware_backend.domain.approval.enums.ApprovalTargetType;
import kr.co.awesomelead.groupware_backend.domain.approval.repository.ApprovalDocumentRepository;
import kr.co.awesomelead.groupware_backend.domain.approval.repository.ApprovalTemplateCategoryRepository;
import kr.co.awesomelead.groupware_backend.domain.approval.repository.ApprovalTemplateLineRepository;
import kr.co.awesomelead.groupware_backend.domain.approval.repository.ApprovalTemplateRepository;
import kr.co.awesomelead.groupware_backend.domain.department.entity.Department;
import kr.co.awesomelead.groupware_backend.domain.department.repository.DepartmentRepository;
import kr.co.awesomelead.groupware_backend.domain.user.entity.User;
import kr.co.awesomelead.groupware_backend.domain.user.enums.Authority;
import kr.co.awesomelead.groupware_backend.domain.user.repository.UserRepository;
import kr.co.awesomelead.groupware_backend.global.error.CustomException;
import kr.co.awesomelead.groupware_backend.global.error.ErrorCode;

import lombok.RequiredArgsConstructor;

import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ApprovalTemplateAdminService {

    private final ApprovalTemplateCategoryRepository approvalTemplateCategoryRepository;
    private final ApprovalTemplateRepository approvalTemplateRepository;
    private final ApprovalTemplateLineRepository approvalTemplateLineRepository;
    private final ApprovalDocumentRepository approvalDocumentRepository;
    private final UserRepository userRepository;
    private final DepartmentRepository departmentRepository;

    @Transactional
    public Long createCategory(Long userId, ApprovalTemplateCategoryCreateRequestDto request) {
        validateAuthority(userId);
        if (approvalTemplateCategoryRepository.existsByCode(request.getCode().trim())) {
            throw new CustomException(ErrorCode.DUPLICATE_APPROVAL_TEMPLATE_CATEGORY_CODE);
        }

        ApprovalTemplateCategory category =
                ApprovalTemplateCategory.builder()
                        .code(request.getCode().trim())
                        .name(request.getName().trim())
                        .sortOrder(request.getSortOrder())
                        .isActive(request.getIsActive())
                        .build();
        return approvalTemplateCategoryRepository.save(category).getId();
    }

    @Transactional(readOnly = true)
    public List<ApprovalTemplateCategoryResponseDto> getCategories(Long userId) {
        validateAuthority(userId);
        return approvalTemplateCategoryRepository.findAllByOrderBySortOrderAscIdAsc().stream()
                .map(ApprovalTemplateCategoryResponseDto::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public ApprovalTemplateCategoryResponseDto getCategory(Long userId, Long categoryId) {
        validateAuthority(userId);
        ApprovalTemplateCategory category = getCategoryEntity(categoryId);
        return ApprovalTemplateCategoryResponseDto.from(category);
    }

    @Transactional
    public void updateCategory(
            Long userId, Long categoryId, ApprovalTemplateCategoryUpdateRequestDto request) {
        validateAuthority(userId);
        ApprovalTemplateCategory category = getCategoryEntity(categoryId);

        String newCode = request.getCode().trim();
        if (!category.getCode().equals(newCode)
                && approvalTemplateCategoryRepository.existsByCode(newCode)) {
            throw new CustomException(ErrorCode.DUPLICATE_APPROVAL_TEMPLATE_CATEGORY_CODE);
        }

        category.setCode(newCode);
        category.setName(request.getName().trim());
        category.setSortOrder(request.getSortOrder());
        category.setIsActive(request.getIsActive());
    }

    @Transactional
    public void deleteCategory(Long userId, Long categoryId) {
        validateAuthority(userId);
        ApprovalTemplateCategory category = getCategoryEntity(categoryId);
        if (approvalTemplateRepository.existsByCategoryId(categoryId)) {
            throw new CustomException(ErrorCode.APPROVAL_TEMPLATE_CATEGORY_IN_USE);
        }
        approvalTemplateCategoryRepository.delete(category);
    }

    @Transactional
    public Long createTemplate(Long userId, ApprovalTemplateCreateRequestDto request) {
        User actor = validateAuthority(userId);
        if (approvalTemplateRepository.existsByCode(request.getCode().trim())) {
            throw new CustomException(ErrorCode.DUPLICATE_APPROVAL_TEMPLATE_CODE);
        }

        ApprovalTemplateCategory category = getCategoryEntity(request.getCategoryId());
        ApprovalTemplate template =
                ApprovalTemplate.builder()
                        .category(category)
                        .code(request.getCode().trim())
                        .name(request.getName().trim())
                        .description(trimToNull(request.getDescription()))
                        .editorType(request.getEditorType())
                        .approvalType(request.getApprovalType())
                        .linePolicy(request.getLinePolicy())
                        .defaultContentDelta(request.getDefaultContentDelta())
                        .isActive(request.getIsActive())
                        .createdBy(actor)
                        .build();

        ApprovalTemplate savedTemplate = approvalTemplateRepository.save(template);
        replaceTemplateLines(savedTemplate, request.getLines());
        return savedTemplate.getId();
    }

    @Transactional(readOnly = true)
    public List<ApprovalTemplateAdminResponseDto> getTemplates(Long userId) {
        validateAuthority(userId);
        List<ApprovalTemplate> templates = approvalTemplateRepository.findAll(Sort.by("id").ascending());
        return templates.stream().map(this::toTemplateResponse).toList();
    }

    @Transactional(readOnly = true)
    public ApprovalTemplateAdminResponseDto getTemplate(Long userId, Long templateId) {
        validateAuthority(userId);
        ApprovalTemplate template = getTemplateEntity(templateId);
        return toTemplateResponse(template);
    }

    @Transactional
    public void updateTemplate(Long userId, Long templateId, ApprovalTemplateUpdateRequestDto request) {
        validateAuthority(userId);
        ApprovalTemplate template = getTemplateEntity(templateId);

        String newCode = request.getCode().trim();
        if (!template.getCode().equals(newCode) && approvalTemplateRepository.existsByCode(newCode)) {
            throw new CustomException(ErrorCode.DUPLICATE_APPROVAL_TEMPLATE_CODE);
        }

        ApprovalTemplateCategory category = getCategoryEntity(request.getCategoryId());

        template.setCategory(category);
        template.setCode(newCode);
        template.setName(request.getName().trim());
        template.setDescription(trimToNull(request.getDescription()));
        template.setEditorType(request.getEditorType());
        template.setApprovalType(request.getApprovalType());
        template.setLinePolicy(request.getLinePolicy());
        template.setDefaultContentDelta(request.getDefaultContentDelta());
        template.setIsActive(request.getIsActive());

        replaceTemplateLines(template, request.getLines());
    }

    @Transactional
    public void deleteTemplate(Long userId, Long templateId) {
        validateAuthority(userId);
        ApprovalTemplate template = getTemplateEntity(templateId);
        if (approvalDocumentRepository.existsByTemplateId(templateId)) {
            throw new CustomException(ErrorCode.APPROVAL_TEMPLATE_IN_USE);
        }
        approvalTemplateLineRepository.deleteByTemplateId(templateId);
        approvalTemplateRepository.delete(template);
    }

    private void replaceTemplateLines(
            ApprovalTemplate template, List<ApprovalTemplateLineUpsertRequestDto> lineRequests) {
        approvalTemplateLineRepository.deleteByTemplateId(template.getId());
        if (lineRequests == null || lineRequests.isEmpty()) {
            if (template.getLinePolicy() == ApprovalLinePolicy.FIXED) {
                throw new CustomException(ErrorCode.INVALID_APPROVAL_STEP);
            }
            return;
        }

        if (template.getLinePolicy() == ApprovalLinePolicy.FIXED
                && lineRequests.stream()
                        .noneMatch(line -> line.getRole() == ApprovalRouteRole.APPROVAL_LINE)) {
            throw new CustomException(ErrorCode.INVALID_APPROVAL_STEP);
        }

        List<ApprovalTemplateLine> lines = new ArrayList<>();
        for (int i = 0; i < lineRequests.size(); i++) {
            ApprovalTemplateLineUpsertRequestDto request = lineRequests.get(i);
            validateLineRequest(request);

            ApprovalTemplateLine line = new ApprovalTemplateLine();
            line.setTemplate(template);
            line.setRole(request.getRole());
            line.setTargetType(request.getTargetType());
            line.setSequenceNo(request.getSequenceNo() != null ? request.getSequenceNo() : i + 1);
            line.setIsRequired(request.getRequired());

            if (request.getTargetType() == ApprovalTargetType.USER) {
                User targetUser = getUser(request.getTargetUserId());
                line.setTargetUser(targetUser);
                line.setTargetDepartment(null);
            } else {
                Department targetDepartment = getDepartment(request.getTargetDepartmentId());
                line.setTargetDepartment(targetDepartment);
                line.setTargetUser(null);
            }
            lines.add(line);
        }

        approvalTemplateLineRepository.saveAll(lines);
    }

    private void validateLineRequest(ApprovalTemplateLineUpsertRequestDto lineRequest) {
        if (lineRequest.getTargetType() == ApprovalTargetType.USER) {
            if (lineRequest.getTargetUserId() == null) {
                throw new CustomException(ErrorCode.INVALID_ARGUMENT);
            }
            return;
        }
        if (lineRequest.getTargetDepartmentId() == null) {
            throw new CustomException(ErrorCode.INVALID_ARGUMENT);
        }
    }

    private ApprovalTemplateAdminResponseDto toTemplateResponse(ApprovalTemplate template) {
        List<ApprovalTemplateLine> lines =
                approvalTemplateLineRepository.findByTemplateIdOrderBySequenceNoAscIdAsc(template.getId());

        List<ApprovalTemplateAdminResponseDto.LineDto> lineDtos =
                lines.stream()
                        .map(
                                line -> {
                                    User targetUser = line.getTargetUser();
                                    Department targetDepartment = line.getTargetDepartment();

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
                                                    ? targetUser.getDepartment()
                                                            .getName()
                                                            .getDescription()
                                                    : null;
                                    String targetDepartmentName =
                                            targetDepartment != null
                                                    ? targetDepartment.getName().getDescription()
                                                    : null;

                                    return ApprovalTemplateAdminResponseDto.LineDto.builder()
                                            .id(line.getId())
                                            .role(line.getRole())
                                            .targetType(line.getTargetType())
                                            .targetUserId(
                                                    targetUser != null ? targetUser.getId() : null)
                                            .targetUserName(targetUserName)
                                            .targetUserPosition(targetUserPosition)
                                            .targetUserDepartmentName(targetUserDepartmentName)
                                            .targetDepartmentId(
                                                    targetDepartment != null
                                                            ? targetDepartment.getId()
                                                            : null)
                                            .targetDepartmentName(targetDepartmentName)
                                            .targetName(
                                                    toTargetName(
                                                            line.getTargetType(),
                                                            targetUser,
                                                            targetDepartment))
                                            .sequenceNo(line.getSequenceNo())
                                            .required(line.getIsRequired())
                                            .build();
                                })
                        .toList();

        return ApprovalTemplateAdminResponseDto.builder()
                .id(template.getId())
                .categoryId(template.getCategory().getId())
                .categoryCode(template.getCategory().getCode())
                .categoryName(template.getCategory().getName())
                .code(template.getCode())
                .name(template.getName())
                .description(template.getDescription())
                .editorType(template.getEditorType())
                .approvalType(template.getApprovalType())
                .linePolicy(template.getLinePolicy())
                .defaultContentDelta(template.getDefaultContentDelta())
                .isActive(template.getIsActive())
                .lines(lineDtos)
                .build();
    }

    private String toTargetName(
            ApprovalTargetType targetType, User targetUser, Department targetDepartment) {
        if (targetType == ApprovalTargetType.DEPARTMENT) {
            return targetDepartment != null ? "[" + targetDepartment.getName().getDescription() + "]" : null;
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

    private User validateAuthority(Long userId) {
        User user = getUser(userId);
        if (!user.hasAuthority(Authority.MANAGE_APPROVAL_LINE)) {
            throw new CustomException(ErrorCode.NO_AUTHORITY_FOR_APPROVAL_CONFIG);
        }
        return user;
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

    private ApprovalTemplateCategory getCategoryEntity(Long categoryId) {
        return approvalTemplateCategoryRepository
                .findById(categoryId)
                .orElseThrow(() -> new CustomException(ErrorCode.APPROVAL_TEMPLATE_CATEGORY_NOT_FOUND));
    }

    private ApprovalTemplate getTemplateEntity(Long templateId) {
        return approvalTemplateRepository
                .findById(templateId)
                .orElseThrow(() -> new CustomException(ErrorCode.APPROVAL_TEMPLATE_NOT_FOUND));
    }

    private String trimToNull(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }
}
