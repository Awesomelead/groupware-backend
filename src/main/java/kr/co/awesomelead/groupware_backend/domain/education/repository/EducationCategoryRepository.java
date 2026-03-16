package kr.co.awesomelead.groupware_backend.domain.education.repository;

import kr.co.awesomelead.groupware_backend.domain.education.entity.EducationCategory;
import kr.co.awesomelead.groupware_backend.domain.education.enums.EducationCategoryType;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface EducationCategoryRepository extends JpaRepository<EducationCategory, Long> {

    Optional<EducationCategory> findByCode(String code);

    List<EducationCategory> findAllByCategoryTypeAndActiveTrueOrderByDepthAscSortOrderAscIdAsc(
            EducationCategoryType categoryType);
}

