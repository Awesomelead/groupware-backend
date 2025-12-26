package kr.co.awesomelead.groupware_backend.domain.visit.repository;

import kr.co.awesomelead.groupware_backend.domain.visit.entity.Visit;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VisitRepository extends JpaRepository<Visit, Long> {

}
