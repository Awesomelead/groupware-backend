package kr.co.awesomelead.groupware_backend.domain.user.repository;

import java.util.List;
import java.util.Optional;
import kr.co.awesomelead.groupware_backend.domain.department.entity.Department;
import kr.co.awesomelead.groupware_backend.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserRepository extends JpaRepository<User, Long> {

    boolean existsByEmail(String email);

    boolean existsByRegistrationNumber(String registrationNumber);

    Optional<User> findByEmail(String username);

    long countByDepartment(Department department);

    @Query("SELECT u FROM User u JOIN FETCH u.department d WHERE d.id IN :departmentIds")
    List<User> findAllByDepartmentIdIn(@Param("departmentIds") List<Long> departmentIds);
}
