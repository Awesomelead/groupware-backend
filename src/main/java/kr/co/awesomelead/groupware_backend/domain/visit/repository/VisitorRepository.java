package kr.co.awesomelead.groupware_backend.domain.visit.repository;

import java.util.Optional;
import kr.co.awesomelead.groupware_backend.domain.visit.entity.Visitor;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VisitorRepository extends JpaRepository<Visitor, Long> {

    Optional<Visitor> findByPhoneNumber(String phoneNumber);
}
