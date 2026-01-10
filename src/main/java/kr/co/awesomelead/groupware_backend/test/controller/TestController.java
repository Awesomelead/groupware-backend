package kr.co.awesomelead.groupware_backend.test.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import jakarta.validation.Valid;

import kr.co.awesomelead.groupware_backend.domain.auth.dto.request.FindEmailRequestDto;
import kr.co.awesomelead.groupware_backend.domain.auth.dto.response.FindEmailResponseDto;
import kr.co.awesomelead.groupware_backend.domain.department.enums.Company;
import kr.co.awesomelead.groupware_backend.domain.user.entity.User;
import kr.co.awesomelead.groupware_backend.domain.user.enums.Role;
import kr.co.awesomelead.groupware_backend.domain.user.enums.Status;
import kr.co.awesomelead.groupware_backend.domain.user.repository.UserRepository;
import kr.co.awesomelead.groupware_backend.test.service.TestService;

import lombok.RequiredArgsConstructor;

import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Profile("test")
@RestController
@RequestMapping("/api/test")
@RequiredArgsConstructor
@Tag(name = "Test", description = """
            ## 개발 중 테스트 API
            """)
public class TestController {

    private final UserRepository userRepository;
    private final BCryptPasswordEncoder passwordEncoder;
    private final TestService testService;

    @Operation(summary = "[테스트] 아이디 찾기 (전체 조회)", description = "성능 테스트용 - 인증 우회")
    @PostMapping("/find-email")
    public ResponseEntity<FindEmailResponseDto> testFindEmail(
            @Valid @RequestBody FindEmailRequestDto requestDto) {

        // 인증 체크 없음
        FindEmailResponseDto response =
                testService.findEmailByAll(requestDto.getName(), requestDto.getPhoneNumber());

        return ResponseEntity.ok(response);
    }

    @Operation(summary = "[테스트] 아이디 찾기 (해시)", description = "성능 테스트용 - 인증 우회")
    @PostMapping("/find-email/hash")
    public ResponseEntity<FindEmailResponseDto> testFindEmailByHash(
            @Valid @RequestBody FindEmailRequestDto requestDto) {

        // 인증 체크 없음
        FindEmailResponseDto response =
                testService.findEmailByHash(requestDto.getName(), requestDto.getPhoneNumber());

        return ResponseEntity.ok(response);
    }

    @Operation(summary = "[테스트] 테스트 user 더미데이터 생성", description = "선택한 수만큼 테스트 user 더미데이터를 생성합니다.")
    @PostMapping("/generate-users/{count}")
    public ResponseEntity<String> generateUsers(@PathVariable int count) {
        for (int i = 1; i <= count; i++) {
            User user =
                    User.builder()
                            .nameKor("홍길동")
                            .nameEng("Hong " + i)
                            .email("testuser" + i + "@example.com")
                            .password(passwordEncoder.encode("test1234!"))
                            .phoneNumber(String.format("0101234%04d", i))
                            .nationality("대한민국")
                            .registrationNumber(String.format("9001%02d-1234567", i % 28 + 1))
                            .workLocation(Company.AWESOME)
                            .role(Role.USER)
                            .status(Status.AVAILABLE)
                            .build();

            userRepository.save(user);
        }

        return ResponseEntity.ok(count + "명의 테스트 사용자 생성 완료!");
    }
}
