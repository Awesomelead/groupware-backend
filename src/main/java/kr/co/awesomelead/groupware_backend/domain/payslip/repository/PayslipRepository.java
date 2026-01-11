package kr.co.awesomelead.groupware_backend.domain.payslip.repository;

import kr.co.awesomelead.groupware_backend.domain.payslip.entity.Payslip;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PayslipRepository extends JpaRepository<Payslip, Long> {

}
