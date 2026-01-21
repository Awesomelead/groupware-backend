package kr.co.awesomelead.groupware_backend.domain.visit.repository;

import kr.co.awesomelead.groupware_backend.domain.visit.entity.VisitRecord;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VisitRecordRepository extends JpaRepository<VisitRecord, Long> {

}
