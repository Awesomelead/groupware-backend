package kr.co.awesomelead.groupware_backend.domain.department.repository;

import java.util.List;
import kr.co.awesomelead.groupware_backend.domain.department.entity.Department;
import kr.co.awesomelead.groupware_backend.domain.department.enums.Company;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DepartmentRepository extends JpaRepository<Department, Long> {

    List<Department> findByParentIsNullAndCompany(Company company);
}
