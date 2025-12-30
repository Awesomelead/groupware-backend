package kr.co.awesomelead.groupware_backend.domain.visit.repository;

import java.util.List;
import kr.co.awesomelead.groupware_backend.domain.visit.entity.Visit;
import kr.co.awesomelead.groupware_backend.domain.visit.entity.Visitor;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VisitRepository extends JpaRepository<Visit, Long> {

    List<Visit> findByVisitor(Visitor visitor);
}
