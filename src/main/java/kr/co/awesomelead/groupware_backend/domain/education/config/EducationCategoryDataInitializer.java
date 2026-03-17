package kr.co.awesomelead.groupware_backend.domain.education.config;

import kr.co.awesomelead.groupware_backend.domain.education.entity.EducationCategory;
import kr.co.awesomelead.groupware_backend.domain.education.enums.EducationCategoryType;
import kr.co.awesomelead.groupware_backend.domain.education.repository.EducationCategoryRepository;

import lombok.RequiredArgsConstructor;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class EducationCategoryDataInitializer implements ApplicationRunner {

    private final EducationCategoryRepository educationCategoryRepository;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        // PSM 루트
        createIfAbsent("PSM_OVERVIEW", "사업개요", EducationCategoryType.PSM, null, 0, 1);
        createIfAbsent("PSM_HAZARDOUS_MATERIAL", "유해위험물질", EducationCategoryType.PSM, null, 0, 2);
        createIfAbsent("PSM_RISK_ASSESSMENT", "위험성평가", EducationCategoryType.PSM, null, 0, 3);
        createIfAbsent("PSM_SAFE_OPERATION", "안전운전계획", EducationCategoryType.PSM, null, 0, 4);
        createIfAbsent("PSM_EMERGENCY_PLAN", "비상조치계획", EducationCategoryType.PSM, null, 0, 5);
    }

    private void createIfAbsent(
            String code,
            String name,
            EducationCategoryType type,
            String parentCode,
            int depth,
            int sortOrder) {
        if (educationCategoryRepository.findByCode(code).isPresent()) {
            return;
        }

        EducationCategory parent = null;
        if (parentCode != null) {
            parent = educationCategoryRepository.findByCode(parentCode).orElse(null);
        }

        EducationCategory category =
                EducationCategory.builder()
                        .code(code)
                        .name(name)
                        .categoryType(type)
                        .parent(parent)
                        .depth(depth)
                        .sortOrder(sortOrder)
                        .active(true)
                        .build();

        educationCategoryRepository.save(category);
    }
}
