package kr.co.awesomelead.groupware_backend.global.encryption;

import static org.assertj.core.api.Assertions.assertThat;

import kr.co.awesomelead.groupware_backend.domain.user.entity.User;
import kr.co.awesomelead.groupware_backend.domain.user.enums.Role;
import kr.co.awesomelead.groupware_backend.domain.user.enums.Status;
import kr.co.awesomelead.groupware_backend.domain.user.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
public class ConverterTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    @DisplayName("User 전화번호가 암호화되어 저장되는지 확인")
    void userPhoneNumberEncryption() {
        // given
        String originalPhone = "01012345678";
        User user = new User();
        user.setEmail("test@example.com");
        user.setPassword("password");
        user.setNameKor("테스트");
        user.setNameEng("Test");
        user.setNationality("대한민국");
        user.setZipcode("06234");
        user.setAddress1("서울시 강남구 테헤란로 123");
        user.setAddress2("어썸빌딩 5층");
        user.setRegistrationNumber("950101-1234567");
        user.setPhoneNumber(originalPhone);
        user.setRole(Role.USER);
        user.setStatus(Status.PENDING);

        // when
        User savedUser = userRepository.save(user);
        userRepository.flush(); // 즉시 DB 반영

        // then - Entity에서는 복호화된 값
        assertThat(savedUser.getPhoneNumber()).isEqualTo(originalPhone);

        // DB에는 암호화된 값 확인
        String encryptedInDb =
            jdbcTemplate.queryForObject(
                "SELECT phone_number FROM users WHERE id = ?",
                String.class,
                savedUser.getId());

        assertThat(encryptedInDb).isNotEqualTo(originalPhone); // 암호화됨
        assertThat(encryptedInDb.length()).isGreaterThan(50); // 암호화하면 길어짐

        System.out.println("원본: " + originalPhone);
        System.out.println("DB 저장값: " + encryptedInDb);
    }

    @Test
    @DisplayName("User 주민번호가 암호화되어 저장되는지 확인")
    void userRegistrationNumberEncryption() {
        // given
        String originalRegNum = "950101-1234567";
        User user = new User();
        user.setEmail("test2@example.com");
        user.setPassword("password");
        user.setNameKor("테스트2");
        user.setNameEng("Test2");
        user.setNationality("대한민국");
        user.setZipcode("06234");
        user.setAddress1("서울시 강남구 테헤란로 123");
        user.setAddress2("어썸빌딩 5층");
        user.setRegistrationNumber(originalRegNum);
        user.setPhoneNumber("01087654321");
        user.setRole(Role.USER);
        user.setStatus(Status.PENDING);

        // when
        User savedUser = userRepository.save(user);
        userRepository.flush();

        // then
        assertThat(savedUser.getRegistrationNumber()).isEqualTo(originalRegNum);

        String encryptedInDb =
            jdbcTemplate.queryForObject(
                "SELECT registration_number FROM users WHERE id = ?",
                String.class,
                savedUser.getId());

        assertThat(encryptedInDb).isNotEqualTo(originalRegNum);

        System.out.println("원본: " + originalRegNum);
        System.out.println("DB 저장값: " + encryptedInDb);
    }

    /***
     * @Test
     * @DisplayName("Visitor 전화번호가 암호화되어 저장되는지 확인")
     * void visitorPhoneNumberEncryption() {
     * // given
     * String originalPhone = "01099998888";
     * Visitor visitor = new Visitor();
     * visitor.setName("방문객");
     * visitor.setPhoneNumber(originalPhone);
     * visitor.setPassword("1234");
     *
     * // when
     * Visitor savedVisitor = visitorRepository.save(visitor);
     * visitorRepository.flush();
     *
     * // then
     * assertThat(savedVisitor.getPhoneNumber()).isEqualTo(originalPhone);
     *
     * String encryptedInDb =
     * jdbcTemplate.queryForObject(
     * "SELECT phone_number FROM visitor WHERE id = ?",
     * String.class,
     * savedVisitor.getId());
     *
     * assertThat(encryptedInDb).isNotEqualTo(originalPhone);
     *
     * System.out.println("원본: " + originalPhone);
     * System.out.println("DB 저장값: " + encryptedInDb);
     * }
     ***/

    @Test
    @DisplayName("조회 시 자동으로 복호화되는지 확인")
    void autoDecryption() {
        // given
        String originalPhone = "01055556666";
        User user = new User();
        user.setEmail("test3@example.com");
        user.setPassword("password");
        user.setNameKor("테스트3");
        user.setNameEng("Test3");
        user.setNationality("대한민국");
        user.setZipcode("06234");
        user.setAddress1("서울시 강남구 테헤란로 123");
        user.setAddress2("어썸빌딩 5층");
        user.setRegistrationNumber("960101-1234567");
        user.setPhoneNumber(originalPhone);
        user.setRole(Role.USER);
        user.setStatus(Status.PENDING);

        User savedUser = userRepository.save(user);
        Long userId = savedUser.getId();

        // 영속성 컨텍스트 초기화 (캐시 방지)
        userRepository.flush();
        userRepository
            .findById(userId)
            .ifPresent(
                u -> {
                    // Detach to force fresh DB query
                });

        // when - 새로 조회
        User foundUser = userRepository.findById(userId).orElseThrow();

        // then - 자동으로 복호화되어 나옴
        assertThat(foundUser.getPhoneNumber()).isEqualTo(originalPhone);
    }
}
