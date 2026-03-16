package kr.co.awesomelead.groupware_backend.domain.education.service;

import kr.co.awesomelead.groupware_backend.domain.education.dto.response.EducationCategoryNodeDto;
import kr.co.awesomelead.groupware_backend.domain.education.entity.EducationCategory;
import kr.co.awesomelead.groupware_backend.domain.education.enums.EducationCategoryType;
import kr.co.awesomelead.groupware_backend.domain.education.repository.EducationCategoryRepository;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class EducationCategoryService {

    private final EducationCategoryRepository educationCategoryRepository;

    @Transactional(readOnly = true)
    public List<EducationCategoryNodeDto> getCategoryTree(EducationCategoryType type) {
        List<EducationCategory> categories =
                educationCategoryRepository
                        .findAllByCategoryTypeAndActiveTrueOrderByDepthAscSortOrderAscIdAsc(type);

        Map<Long, EducationCategoryNodeDto> nodeById = new HashMap<>();
        List<EducationCategoryNodeDto> roots = new ArrayList<>();

        for (EducationCategory category : categories) {
            EducationCategoryNodeDto node =
                    EducationCategoryNodeDto.builder()
                            .id(category.getId())
                            .code(category.getCode())
                            .name(category.getName())
                            .children(new ArrayList<>())
                            .build();
            nodeById.put(category.getId(), node);
        }

        for (EducationCategory category : categories) {
            EducationCategoryNodeDto node = nodeById.get(category.getId());
            if (category.getParent() == null) {
                roots.add(node);
                continue;
            }
            EducationCategoryNodeDto parentNode = nodeById.get(category.getParent().getId());
            if (parentNode != null) {
                parentNode.getChildren().add(node);
            }
        }
        return roots;
    }
}
