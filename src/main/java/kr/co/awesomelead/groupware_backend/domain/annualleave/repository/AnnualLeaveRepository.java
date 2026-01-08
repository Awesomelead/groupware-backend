package kr.co.awesomelead.groupware_backend.domain.annualleave.repository;

import kr.co.awesomelead.groupware_backend.domain.annualleave.entity.AnnualLeave;
import kr.co.awesomelead.groupware_backend.domain.user.entity.User;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AnnualLeaveRepository extends JpaRepository<AnnualLeave, Long> {

    Optional<AnnualLeave> findByUser(User user);
}
