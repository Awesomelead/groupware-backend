package kr.co.awesomelead.groupware_backend.domain.user;

import static org.assertj.core.api.Assertions.assertThat;

import kr.co.awesomelead.groupware_backend.domain.user.entity.MyInfoUpdateRequest;
import kr.co.awesomelead.groupware_backend.domain.user.entity.User;
import kr.co.awesomelead.groupware_backend.domain.user.enums.MyInfoUpdateRequestStatus;
import kr.co.awesomelead.groupware_backend.domain.user.enums.Role;
import kr.co.awesomelead.groupware_backend.domain.user.enums.Status;
import kr.co.awesomelead.groupware_backend.domain.user.repository.MyInfoUpdateRequestRepository;
import kr.co.awesomelead.groupware_backend.domain.user.repository.UserRepository;
import kr.co.awesomelead.groupware_backend.domain.user.repository.querydsl.UserQueryRepository;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
@DisplayName("UserQueryRepository 클래스의")
class UserQueryRepositoryTest {

    @Autowired private UserRepository userRepository;
    @Autowired private MyInfoUpdateRequestRepository myInfoUpdateRequestRepository;
    @Autowired private UserQueryRepository userQueryRepository;

    @Test
    @DisplayName("findAllForAdminWithFilters 메서드는 MASTER_ADMIN을 제외한다")
    void findAllForAdminWithFilters_excludes_master_admin() {
        // given
        User user =
                createUser("user@example.com", "일반사용자", "900101-1234567", "01011112222", Role.USER);
        User admin =
                createUser("admin@example.com", "관리자", "900102-1234567", "01022223333", Role.ADMIN);
        createUser(
                "master@example.com", "마스터관리자", "900103-1234567", "01033334444", Role.MASTER_ADMIN);
        userRepository.saveAll(List.of(user, admin));
        userRepository.save(
                createUser(
                        "saved-master@example.com",
                        "저장된마스터관리자",
                        "900104-1234567",
                        "01044445555",
                        Role.MASTER_ADMIN));

        // when
        var result =
                userQueryRepository.findAllForAdminWithFilters(
                        null, null, null, null, null, null, null, null, PageRequest.of(0, 20));

        // then
        assertThat(result.getContent())
                .extracting(User::getRole)
                .containsExactlyInAnyOrder(Role.USER, Role.ADMIN);
    }

    @Test
    @DisplayName("findAllForAdminWithFilters 메서드는 개인정보 수정 승인 대기 여부로 필터링한다")
    void findAllForAdminWithFilters_filters_by_pending_my_info_request() {
        // given
        User userWithPendingRequest =
                userRepository.save(
                        createUser(
                                "pending-my-info@example.com",
                                "수정요청사용자",
                                "900109-1234567",
                                "01099990000",
                                Role.USER));
        User userWithoutPendingRequest =
                userRepository.save(
                        createUser(
                                "no-pending-my-info@example.com",
                                "일반사용자",
                                "900110-1234567",
                                "01099990001",
                                Role.USER));
        myInfoUpdateRequestRepository.save(
                MyInfoUpdateRequest.builder()
                        .user(userWithPendingRequest)
                        .requestedAddress1("서울시 강남구 테헤란로 456")
                        .status(MyInfoUpdateRequestStatus.PENDING)
                        .build());

        // when
        var pendingResult =
                userQueryRepository.findAllForAdminWithFilters(
                        null, null, null, null, null, null, null, true, PageRequest.of(0, 20));
        var noPendingResult =
                userQueryRepository.findAllForAdminWithFilters(
                        null, null, null, null, null, null, null, false, PageRequest.of(0, 20));

        // then
        assertThat(pendingResult.getContent())
                .extracting(User::getId)
                .containsExactly(userWithPendingRequest.getId());
        assertThat(noPendingResult.getContent())
                .extracting(User::getId)
                .containsExactly(userWithoutPendingRequest.getId());
    }

    @Test
    @DisplayName("findAllForAdminWithFiltersNoPaging 메서드는 MASTER_ADMIN을 제외한다")
    void findAllForAdminWithFiltersNoPaging_excludes_master_admin() {
        // given
        userRepository.save(
                createUser(
                        "user-excel@example.com",
                        "엑셀사용자",
                        "900105-1234567",
                        "01055556666",
                        Role.USER));
        userRepository.save(
                createUser(
                        "master-excel@example.com",
                        "엑셀마스터관리자",
                        "900106-1234567",
                        "01066667777",
                        Role.MASTER_ADMIN));

        // when
        List<User> result =
                userQueryRepository.findAllForAdminWithFiltersNoPaging(
                        null, null, null, null, null, null, null);

        // then
        assertThat(result).extracting(User::getRole).containsExactly(Role.USER);
    }

    @Test
    @DisplayName("findAllAvailableWithFilters 메서드는 MASTER_ADMIN을 제외한다")
    void findAllAvailableWithFilters_excludes_master_admin() {
        // given
        userRepository.save(
                createUser(
                        "user-list@example.com",
                        "목록사용자",
                        "900107-1234567",
                        "01077778888",
                        Role.USER));
        userRepository.save(
                createUser(
                        "master-list@example.com",
                        "목록마스터관리자",
                        "900108-1234567",
                        "01088889999",
                        Role.MASTER_ADMIN));

        // when
        var result =
                userQueryRepository.findAllAvailableWithFilters(
                        null, null, null, null, null, null, PageRequest.of(0, 20));

        // then
        assertThat(result.getContent()).extracting(User::getRole).containsExactly(Role.USER);
    }

    private User createUser(
            String email,
            String nameKor,
            String registrationNumber,
            String phoneNumber,
            Role role) {
        User user = new User();
        user.setEmail(email);
        user.setPassword("password");
        user.setNameKor(nameKor);
        user.setNameEng(nameKor);
        user.setNationality("대한민국");
        user.setZipcode("06234");
        user.setAddress1("서울시 강남구 테헤란로 123");
        user.setAddress2("어썸빌딩 5층");
        user.setRegistrationNumber(registrationNumber);
        user.setPhoneNumber(phoneNumber);
        user.setRole(role);
        user.setStatus(Status.AVAILABLE);
        return user;
    }
}
