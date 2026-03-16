package kr.co.awesomelead.groupware_backend.domain.education.service;

import kr.co.awesomelead.groupware_backend.domain.education.dto.request.EducationCategoryCreateRequestDto;
import kr.co.awesomelead.groupware_backend.domain.education.dto.request.EducationCategoryReorderRequestDto;
import kr.co.awesomelead.groupware_backend.domain.education.dto.request.EducationCategoryUpdateRequestDto;
import kr.co.awesomelead.groupware_backend.domain.education.entity.EducationCategory;
import kr.co.awesomelead.groupware_backend.domain.education.repository.EducationCategoryRepository;
import kr.co.awesomelead.groupware_backend.domain.user.entity.User;
import kr.co.awesomelead.groupware_backend.domain.user.enums.Role;
import kr.co.awesomelead.groupware_backend.domain.user.repository.UserRepository;
import kr.co.awesomelead.groupware_backend.global.error.CustomException;
import kr.co.awesomelead.groupware_backend.global.error.ErrorCode;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdminEducationCategoryService {

    private final EducationCategoryRepository educationCategoryRepository;
    private final UserRepository userRepository;

    @Transactional
    public Long createCategory(Long adminId, EducationCategoryCreateRequestDto requestDto) {
        validateCategoryManageAuthority(adminId);

        if (educationCategoryRepository.existsByCode(requestDto.getCode().trim())) {
            throw new CustomException(ErrorCode.DUPLICATE_EDUCATION_CATEGORY_CODE);
        }

        EducationCategory parent = null;
        int depth = 0;
        if (requestDto.getParentId() != null) {
            parent =
                    educationCategoryRepository
                            .findById(requestDto.getParentId())
                            .orElseThrow(
                                    () -> new CustomException(ErrorCode.EDUCATION_CATEGORY_NOT_FOUND));
            if (parent.getCategoryType() != requestDto.getCategoryType()) {
                throw new CustomException(ErrorCode.INVALID_ARGUMENT);
            }
            depth = parent.getDepth() + 1;
        }

        EducationCategory category =
                EducationCategory.builder()
                        .name(requestDto.getName().trim())
                        .code(requestDto.getCode().trim())
                        .categoryType(requestDto.getCategoryType())
                        .parent(parent)
                        .depth(depth)
                        .sortOrder(requestDto.getSortOrder() != null ? requestDto.getSortOrder() : 0)
                        .active(true)
                        .build();

        return educationCategoryRepository.save(category).getId();
    }

    @Transactional
    public void updateCategory(
            Long adminId, Long categoryId, EducationCategoryUpdateRequestDto requestDto) {
        validateCategoryManageAuthority(adminId);

        EducationCategory category =
                educationCategoryRepository
                        .findById(categoryId)
                        .orElseThrow(() -> new CustomException(ErrorCode.EDUCATION_CATEGORY_NOT_FOUND));

        String newCode = requestDto.getCode().trim();
        if (!newCode.equals(category.getCode()) && educationCategoryRepository.existsByCode(newCode)) {
            throw new CustomException(ErrorCode.DUPLICATE_EDUCATION_CATEGORY_CODE);
        }

        EducationCategory parent = null;
        int depth = 0;
        if (requestDto.getParentId() != null) {
            parent =
                    educationCategoryRepository
                            .findById(requestDto.getParentId())
                            .orElseThrow(
                                    () -> new CustomException(ErrorCode.EDUCATION_CATEGORY_NOT_FOUND));
            if (parent.getCategoryType() != category.getCategoryType()) {
                throw new CustomException(ErrorCode.INVALID_ARGUMENT);
            }
            if (parent.getId().equals(category.getId())) {
                throw new CustomException(ErrorCode.INVALID_ARGUMENT);
            }
            depth = parent.getDepth() + 1;
        }

        category.setName(requestDto.getName().trim());
        category.setCode(newCode);
        category.setParent(parent);
        category.setDepth(depth);
        if (requestDto.getSortOrder() != null) {
            category.setSortOrder(requestDto.getSortOrder());
        }
    }

    @Transactional
    public void deactivateCategory(Long adminId, Long categoryId) {
        validateCategoryManageAuthority(adminId);

        EducationCategory category =
                educationCategoryRepository
                        .findById(categoryId)
                        .orElseThrow(() -> new CustomException(ErrorCode.EDUCATION_CATEGORY_NOT_FOUND));
        category.setActive(false);
    }

    @Transactional
    public void reorderCategories(Long adminId, EducationCategoryReorderRequestDto requestDto) {
        validateCategoryManageAuthority(adminId);

        List<Long> ids = requestDto.getCategoryIds();
        Set<Long> distinctIds = new HashSet<>(ids);
        if (distinctIds.size() != ids.size()) {
            throw new CustomException(ErrorCode.INVALID_ARGUMENT);
        }

        List<EducationCategory> categories = educationCategoryRepository.findAllById(ids);
        if (categories.size() != ids.size()) {
            throw new CustomException(ErrorCode.EDUCATION_CATEGORY_NOT_FOUND);
        }

        Set<Long> parentIds =
                categories.stream()
                        .map(c -> c.getParent() == null ? null : c.getParent().getId())
                        .collect(Collectors.toSet());
        if (parentIds.size() != 1) {
            throw new CustomException(ErrorCode.INVALID_ARGUMENT);
        }

        Map<Long, EducationCategory> categoryById =
                categories.stream().collect(Collectors.toMap(EducationCategory::getId, Function.identity()));

        for (int i = 0; i < ids.size(); i++) {
            Long id = ids.get(i);
            categoryById.get(id).setSortOrder(i + 1);
        }
    }

    private void validateCategoryManageAuthority(Long adminId) {
        User admin =
                userRepository
                        .findById(adminId)
                        .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
        if (admin.getRole() != Role.ADMIN && admin.getRole() != Role.MASTER_ADMIN) {
            throw new CustomException(ErrorCode.NO_AUTHORITY_FOR_EDUCATION_CATEGORY_MANAGE);
        }
    }
}
