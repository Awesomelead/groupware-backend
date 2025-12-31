package kr.co.awesomelead.groupware_backend.domain.department.repository;

import kr.co.awesomelead.groupware_backend.domain.department.entity.Department;

import org.springframework.data.jpa.repository.JpaRepository;

public interface DepartmentRepository extends JpaRepository<Department, Long> {}
