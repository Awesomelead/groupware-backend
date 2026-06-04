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
        createOrUpdate("PSM_OVERVIEW", "사업개요", EducationCategoryType.PSM, null, 0, 1);
        createOrUpdate("PSM_HAZARDOUS_MATERIAL", "유해위험물질", EducationCategoryType.PSM, null, 0, 2);
        createOrUpdate("PSM_RISK_ASSESSMENT", "위험성평가", EducationCategoryType.PSM, null, 0, 3);
        createOrUpdate("PSM_SAFE_OPERATION", "안전운전계획", EducationCategoryType.PSM, null, 0, 4);
        createOrUpdate("PSM_EMERGENCY_PLAN", "비상조치계획", EducationCategoryType.PSM, null, 0, 5);

        // 안전보건 루트
        createOrUpdate("SAFETY_EDUCATION", "안전보건교육", EducationCategoryType.SAFETY, null, 0, 1);
        createOrUpdate("SAFETY_RESOURCE", "안전보건 자료", EducationCategoryType.SAFETY, null, 0, 2);

        createOrUpdate(
                "SAFETY_EDUCATION_PLAN",
                "안전보건교육계획서",
                EducationCategoryType.SAFETY,
                "SAFETY_EDUCATION",
                1,
                1);
        createOrUpdate(
                "SAFETY_EDUCATION_LOG_ATTENDEE",
                "안전보건교육 교육일지 및 참석자 명단",
                EducationCategoryType.SAFETY,
                "SAFETY_EDUCATION",
                1,
                2);

        createOrUpdate(
                "SAFETY_MANAGEMENT_POLICY",
                "안전보건경영방침",
                EducationCategoryType.SAFETY,
                "SAFETY_RESOURCE",
                1,
                1);
        createOrUpdate(
                "SAFETY_MANAGEMENT_REGULATION",
                "안전보건관리규정",
                EducationCategoryType.SAFETY,
                "SAFETY_RESOURCE",
                1,
                2);
        createOrUpdate(
                "SAFETY_AIR_ENV_MEASUREMENT_RECORD",
                "대기환경측정기록부",
                EducationCategoryType.SAFETY,
                "SAFETY_RESOURCE",
                1,
                3);
        createOrUpdate(
                "SAFETY_WORK_ENV_MEASUREMENT_RECORD",
                "작업환경측정기록부",
                EducationCategoryType.SAFETY,
                "SAFETY_RESOURCE",
                1,
                4);
        createOrUpdate(
                "SAFETY_CHEMICAL_ACCIDENT_PREVENTION_PLAN",
                "화학사고예방관리계획서",
                EducationCategoryType.SAFETY,
                "SAFETY_RESOURCE",
                1,
                5);
        createOrUpdate(
                "SAFETY_INDUSTRIAL_SAFETY_HEALTH_COMMITTEE",
                "산업안전보건위원회(게시판)",
                EducationCategoryType.SAFETY,
                "SAFETY_RESOURCE",
                1,
                6);
    }

    private void createOrUpdate(
            String code,
            String name,
            EducationCategoryType type,
            String parentCode,
            int depth,
            int sortOrder) {
        EducationCategory parent = null;
        if (parentCode != null) {
            parent =
                    educationCategoryRepository
                            .findByCode(parentCode)
                            .orElseThrow(
                                    () ->
                                            new IllegalStateException(
                                                    "교육 카테고리 부모 코드를 찾을 수 없습니다: "
                                                            + parentCode));
        }

        EducationCategory category =
                educationCategoryRepository.findByCode(code).orElse(null);
        if (category != null) {
            category.setName(name);
            category.setCategoryType(type);
            category.setParent(parent);
            category.setDepth(depth);
            category.setSortOrder(sortOrder);
            category.setActive(true);
            return;
        }

        category =
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
