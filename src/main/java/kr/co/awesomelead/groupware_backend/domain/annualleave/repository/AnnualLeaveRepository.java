package kr.co.awesomelead.groupware_backend.domain.annualleave.repository;

import kr.co.awesomelead.groupware_backend.domain.annualleave.entity.AnnualLeave;
import kr.co.awesomelead.groupware_backend.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AnnualLeaveRepository extends JpaRepository<AnnualLeave, Long> {

    AnnualLeave findByUser(User user);
}
