package kr.co.awesomelead.groupware_backend.domain.requesthistory.repository;

import kr.co.awesomelead.groupware_backend.domain.requesthistory.entity.RequestHistory;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RequestHistoryRepository extends JpaRepository<RequestHistory, Long> {

    List<RequestHistory> findByUserIdOrderByRequestDateDescIdDesc(Long userId);

    Optional<RequestHistory> findByIdAndUserId(Long id, Long userId);
}
