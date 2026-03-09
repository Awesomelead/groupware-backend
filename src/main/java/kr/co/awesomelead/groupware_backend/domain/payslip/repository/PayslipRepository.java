package kr.co.awesomelead.groupware_backend.domain.payslip.repository;

import kr.co.awesomelead.groupware_backend.domain.payslip.entity.Payslip;
import kr.co.awesomelead.groupware_backend.domain.payslip.enums.PayslipStatus;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface PayslipRepository extends JpaRepository<Payslip, Long> {

    @Query(
            "SELECT p FROM Payslip p JOIN FETCH p.user "
                    + "WHERE (:status IS NULL OR p.status = :status)")
    List<Payslip> findAllByStatusOptionalWithUser(@Param("status") PayslipStatus status);

    List<Payslip> findAllByUserIdOrderByCreatedAtDesc(Long userId);
}
