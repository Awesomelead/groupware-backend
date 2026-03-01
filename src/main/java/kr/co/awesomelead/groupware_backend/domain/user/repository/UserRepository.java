package kr.co.awesomelead.groupware_backend.domain.user.repository;

import kr.co.awesomelead.groupware_backend.domain.department.entity.Department;
import kr.co.awesomelead.groupware_backend.domain.department.enums.Company;
import kr.co.awesomelead.groupware_backend.domain.user.entity.User;
import kr.co.awesomelead.groupware_backend.domain.user.enums.JobType;
import kr.co.awesomelead.groupware_backend.domain.user.enums.Position;
import kr.co.awesomelead.groupware_backend.domain.user.enums.Role;
import kr.co.awesomelead.groupware_backend.domain.user.enums.Status;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    boolean existsByEmail(String email);

    boolean existsByRegistrationNumber(String registrationNumber);

    Optional<User> findByEmail(String username);

    long countByDepartment(Department department);

    @Query("SELECT u FROM User u JOIN FETCH u.department d WHERE d.id IN :departmentIds")
    List<User> findAllByDepartmentIdIn(@Param("departmentIds") List<Long> departmentIds);

    List<User> findAllByNameKor(String nameKor);

    Optional<User> findByPhoneNumberHash(String phoneNumberHash);

    @Query(
            "SELECT u FROM User u WHERE (u.nameKor = :name OR u.nameEng = :name) AND u.hireDate ="
                    + " :joinDate")
    Optional<User> findByNameAndJoinDate(
            @Param("name") String name, @Param("joinDate") LocalDate joinDate);

    boolean existsByPhoneNumberHash(String phoneNumberHash);

    @Query("SELECT u.id FROM User u WHERE u.workLocation = :company")
    List<Long> findAllIdsByCompany(@Param("company") Company company);

    @Query(
            "SELECT u FROM User u LEFT JOIN FETCH u.department d WHERE u.status = :status ORDER BY"
                    + " u.id DESC")
    List<User> findAllByStatusWithDepartment(@Param("status") Status status);

    @Query(
            value =
                    "SELECT u FROM User u LEFT JOIN FETCH u.department d "
                            + "WHERE (:keyword IS NULL OR :keyword = '' "
                            + "OR lower(u.nameKor) LIKE lower(concat('%', :keyword, '%')) "
                            + "OR lower(u.nameEng) LIKE lower(concat('%', :keyword, '%')) "
                            + "OR lower(u.email) LIKE lower(concat('%', :keyword, '%'))) "
                            + "AND (:position IS NULL OR u.position = :position) "
                            + "AND (:departmentId IS NULL OR u.department.id = :departmentId) "
                            + "AND (:jobType IS NULL OR u.jobType = :jobType) "
                            + "AND (:role IS NULL OR u.role = :role) "
                            + "ORDER BY u.id DESC",
            countQuery =
                    "SELECT count(u) FROM User u "
                            + "WHERE (:keyword IS NULL OR :keyword = '' "
                            + "OR lower(u.nameKor) LIKE lower(concat('%', :keyword, '%')) "
                            + "OR lower(u.nameEng) LIKE lower(concat('%', :keyword, '%')) "
                            + "OR lower(u.email) LIKE lower(concat('%', :keyword, '%'))) "
                            + "AND (:position IS NULL OR u.position = :position) "
                            + "AND (:departmentId IS NULL OR u.department.id = :departmentId) "
                            + "AND (:jobType IS NULL OR u.jobType = :jobType) "
                            + "AND (:role IS NULL OR u.role = :role)")
    Page<User> findAllWithDepartmentAndKeyword(
            @Param("keyword") String keyword,
            @Param("position") Position position,
            @Param("departmentId") Long departmentId,
            @Param("jobType") JobType jobType,
            @Param("role") Role role,
            Pageable pageable);
}
