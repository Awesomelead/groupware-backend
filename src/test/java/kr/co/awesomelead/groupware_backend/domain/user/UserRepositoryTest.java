package kr.co.awesomelead.groupware_backend.domain.user;

import static org.assertj.core.api.Assertions.assertThat;

import kr.co.awesomelead.groupware_backend.domain.department.entity.Department;
import kr.co.awesomelead.groupware_backend.domain.department.enums.Company;
import kr.co.awesomelead.groupware_backend.domain.department.enums.DepartmentName;
import kr.co.awesomelead.groupware_backend.domain.department.repository.DepartmentRepository;
import kr.co.awesomelead.groupware_backend.domain.user.entity.User;
import kr.co.awesomelead.groupware_backend.domain.user.enums.Position;
import kr.co.awesomelead.groupware_backend.domain.user.enums.Role;
import kr.co.awesomelead.groupware_backend.domain.user.enums.Status;
import kr.co.awesomelead.groupware_backend.domain.user.repository.UserRepository;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
@DisplayName("UserRepository 클래스의")
class UserRepositoryTest {

    @Autowired private UserRepository userRepository;
    @Autowired private DepartmentRepository departmentRepository;

    private int userSequence = 0;

    @Test
    @DisplayName("findAllActiveUserIds 메서드는 MASTER_ADMIN을 제외한다")
    void findAllActiveUserIds_excludes_master_admin() {
        User user =
                saveUser(
                        "active-user",
                        Role.USER,
                        Status.AVAILABLE,
                        Company.AWESOME,
                        Position.STAFF,
                        null);
        User admin =
                saveUser(
                        "active-admin",
                        Role.ADMIN,
                        Status.AVAILABLE,
                        Company.AWESOME,
                        Position.MANAGER,
                        null);
        User masterAdmin =
                saveUser(
                        "active-master",
                        Role.MASTER_ADMIN,
                        Status.AVAILABLE,
                        Company.AWESOME,
                        Position.STAFF,
                        null);

        List<Long> result = userRepository.findAllActiveUserIds();

        assertThat(result)
                .contains(user.getId(), admin.getId())
                .doesNotContain(masterAdmin.getId());
    }

    @Test
    @DisplayName("findAllIdsByCompany 메서드는 MASTER_ADMIN을 제외한다")
    void findAllIdsByCompany_excludes_master_admin() {
        User user =
                saveUser(
                        "company-user",
                        Role.USER,
                        Status.AVAILABLE,
                        Company.AWESOME,
                        Position.STAFF,
                        null);
        User masterAdmin =
                saveUser(
                        "company-master",
                        Role.MASTER_ADMIN,
                        Status.AVAILABLE,
                        Company.AWESOME,
                        Position.STAFF,
                        null);
        User otherCompanyUser =
                saveUser(
                        "other-company-user",
                        Role.USER,
                        Status.AVAILABLE,
                        Company.MARUI,
                        Position.STAFF,
                        null);

        List<Long> result = userRepository.findAllIdsByCompany(Company.AWESOME);

        assertThat(result)
                .contains(user.getId())
                .doesNotContain(masterAdmin.getId(), otherCompanyUser.getId());
    }

    @Test
    @DisplayName("findAllByDepartmentIdIn 메서드는 MASTER_ADMIN을 제외한다")
    void findAllByDepartmentIdIn_excludes_master_admin() {
        Department department = saveDepartment();
        User user =
                saveUser(
                        "department-user",
                        Role.USER,
                        Status.AVAILABLE,
                        Company.AWESOME,
                        Position.STAFF,
                        department);
        User masterAdmin =
                saveUser(
                        "department-master",
                        Role.MASTER_ADMIN,
                        Status.AVAILABLE,
                        Company.AWESOME,
                        Position.STAFF,
                        department);

        List<User> result = userRepository.findAllByDepartmentIdIn(List.of(department.getId()));

        assertThat(result)
                .extracting(User::getId)
                .contains(user.getId())
                .doesNotContain(masterAdmin.getId());
    }

    @Test
    @DisplayName("findAllByCompanyAndStatusExcludingPosition 메서드는 MASTER_ADMIN을 제외한다")
    void findAllByCompanyAndStatusExcludingPosition_excludes_master_admin() {
        User user =
                saveUser(
                        "safety-user",
                        Role.USER,
                        Status.AVAILABLE,
                        Company.AWESOME,
                        Position.STAFF,
                        null);
        User masterAdmin =
                saveUser(
                        "safety-master",
                        Role.MASTER_ADMIN,
                        Status.AVAILABLE,
                        Company.AWESOME,
                        Position.STAFF,
                        null);
        User ceo =
                saveUser(
                        "safety-ceo",
                        Role.USER,
                        Status.AVAILABLE,
                        Company.AWESOME,
                        Position.CEO,
                        null);

        List<User> result =
                userRepository.findAllByCompanyAndStatusExcludingPosition(
                        Company.AWESOME, Status.AVAILABLE, Position.CEO);

        assertThat(result)
                .extracting(User::getId)
                .contains(user.getId())
                .doesNotContain(masterAdmin.getId(), ceo.getId());
    }

    private Department saveDepartment() {
        Department department =
                Department.builder()
                        .name(DepartmentName.CHAMBER_PROD)
                        .company(Company.AWESOME)
                        .build();
        return departmentRepository.save(department);
    }

    private User saveUser(
            String key,
            Role role,
            Status status,
            Company workLocation,
            Position position,
            Department department) {
        int sequence = ++userSequence;
        User user = new User();
        user.setEmail(key + "@example.com");
        user.setPassword("password");
        user.setNameKor(key);
        user.setNameEng(key);
        user.setNationality("대한민국");
        user.setZipcode("06234");
        user.setAddress1("서울시 강남구 테헤란로 123");
        user.setAddress2("어썸빌딩 5층");
        user.setRegistrationNumber(String.format("9001%02d1%06d", sequence, sequence));
        user.setPhoneNumber(String.format("010%08d", sequence));
        user.setRole(role);
        user.setStatus(status);
        user.setWorkLocation(workLocation);
        user.setPosition(position);
        user.setDepartment(department);
        return userRepository.save(user);
    }
}
