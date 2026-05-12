package kr.co.awesomelead.groupware_backend.domain.visit.repository;

import kr.co.awesomelead.groupware_backend.domain.visit.entity.Visit;
import kr.co.awesomelead.groupware_backend.domain.visit.enums.VisitCategory;
import kr.co.awesomelead.groupware_backend.domain.visit.enums.VisitStatus;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface VisitRepository extends JpaRepository<Visit, Long> {

    List<Visit> findByVisitorNameAndPhoneNumberHashOrderByIdDesc(String name, String inputPhoneHash);

    List<Visit> findAllByVisitCategoryAndEndDateBeforeAndStatusNot(
            VisitCategory visitCategory, LocalDate date, VisitStatus status);
}
