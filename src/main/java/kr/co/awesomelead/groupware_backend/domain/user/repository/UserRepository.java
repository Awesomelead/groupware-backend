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

    @Query(
            "SELECT u FROM User u JOIN FETCH u.department d WHERE d.id IN :departmentIds AND"
                    + " u.role <> 'MASTER_ADMIN'")
    List<User> findAllByDepartmentIdIn(@Param("departmentIds") List<Long> departmentIds);

    List<User> findAllByNameKor(String nameKor);

    Optional<User> findByPhoneNumberHash(String phoneNumberHash);

    @Query(
            "SELECT u FROM User u WHERE (u.nameKor = :name OR u.nameEng = :name) AND u.hireDate ="
                    + " :joinDate")
    Optional<User> findByNameAndJoinDate(
            @Param("name") String name, @Param("joinDate") LocalDate joinDate);

    @Query(
            "SELECT u FROM User u WHERE (u.nameKor = :name OR u.nameEng = :name) AND u.birthDate ="
                    + " :birthDate")
    Optional<User> findByNameAndBirthDate(
            @Param("name") String name, @Param("birthDate") LocalDate birthDate);

    @Query(
            "SELECT u FROM User u WHERE (u.nameKor = :name OR u.nameEng = :name) AND u.birthDate ="
                    + " :birthDate")
    List<User> findAllByNameAndBirthDate(
            @Param("name") String name, @Param("birthDate") LocalDate birthDate);

    boolean existsByPhoneNumberHash(String phoneNumberHash);

    List<User> findAllByRole(Role role);

    boolean existsByRole(Role role);

    @Query("SELECT u.id FROM User u WHERE u.department.id = :departmentId")
    List<Long> findAllIdsByDepartmentId(@Param("departmentId") Long departmentId);

    @Query("SELECT u.id FROM User u WHERE u.status = 'AVAILABLE' AND u.role <> 'MASTER_ADMIN'")
    List<Long> findAllActiveUserIds();

    @Query("SELECT u.id FROM User u WHERE u.workLocation = :company AND u.role <> 'MASTER_ADMIN'")
    List<Long> findAllIdsByCompany(@Param("company") Company company);

    @Query(
            "SELECT u.id FROM User u WHERE u.workLocation = :company AND u.jobType = :jobType AND"
                    + " u.role <> 'MASTER_ADMIN'")
    List<Long> findAllIdsByCompanyAndJobType(
            @Param("company") Company company, @Param("jobType") JobType jobType);

    @Query(
            "SELECT u FROM User u "
                    + "WHERE u.workLocation = :company "
                    + "AND u.status = :status "
                    + "AND u.position <> :excludedPosition "
                    + "AND u.role <> 'MASTER_ADMIN'")
    List<User> findAllByCompanyAndStatusExcludingPosition(
            @Param("company") Company company,
            @Param("status") Status status,
            @Param("excludedPosition") Position excludedPosition);

    @Query(
            "SELECT u FROM User u LEFT JOIN FETCH u.department d WHERE u.status = :status ORDER BY"
                    + " u.id DESC")
    List<User> findAllByStatusWithDepartment(@Param("status") Status status);

    @Query(
            value = "SELECT u FROM User u LEFT JOIN FETCH u.department d WHERE u.status = :status",
            countQuery = "SELECT count(u) FROM User u WHERE u.status = :status")
    Page<User> findAllByStatusWithDepartment(@Param("status") Status status, Pageable pageable);

    @Query(
            value =
                    "SELECT u.* FROM users u "
                            + "WHERE u.status = 'AVAILABLE' "
                            + "AND MATCH(u.name_kor) AGAINST(:keyword IN BOOLEAN MODE) "
                            + "ORDER BY u.id DESC",
            countQuery =
                    "SELECT count(*) FROM users u "
                            + "WHERE u.status = 'AVAILABLE' "
                            + "AND MATCH(u.name_kor) AGAINST(:keyword IN BOOLEAN MODE)",
            nativeQuery = true)
    Page<User> searchByNameKorFullText(@Param("keyword") String keyword, Pageable pageable);
}
