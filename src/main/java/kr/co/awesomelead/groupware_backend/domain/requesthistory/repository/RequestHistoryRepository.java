package kr.co.awesomelead.groupware_backend.domain.requesthistory.repository;


import kr.co.awesomelead.groupware_backend.domain.requesthistory.entity.RequestHistory;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RequestHistoryRepository extends JpaRepository<RequestHistory, Long> {

}
