package kr.co.awesomelead.groupware_backend.test.service;

import java.util.List;
import kr.co.awesomelead.groupware_backend.domain.auth.dto.response.FindEmailResponseDto;
import kr.co.awesomelead.groupware_backend.domain.user.entity.User;
import kr.co.awesomelead.groupware_backend.domain.user.repository.UserRepository;
import kr.co.awesomelead.groupware_backend.global.error.CustomException;
import kr.co.awesomelead.groupware_backend.global.error.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class TestService {

    private final UserRepository userRepository;

    // 테스트용 (인증 없음)

    // 아이디 찾기 - 전체 조회 (인증 우회)
    public FindEmailResponseDto findEmailByAll(String name, String phoneNumber) {
        long startTime = System.nanoTime();

        List<User> users = userRepository.findAllByNameKor(name);
        User user =
            users.stream()
                .filter(u -> u.getPhoneNumber().equals(phoneNumber))
                .findFirst()
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        long endTime = System.nanoTime();
        log.info("[전체조회] 조회 {}명, 소요시간: {}ms", users.size(), (endTime - startTime) / 1_000_000);

        return new FindEmailResponseDto(maskEmail(user.getEmail()));
    }

    // 아이디 찾기 - 해시 검색 (인증 우회)
    public FindEmailResponseDto findEmailByHash(String name, String phoneNumber) {
        long startTime = System.nanoTime();

        String phoneNumberHash = User.hashValue(phoneNumber);
        User user =
            userRepository
                .findByPhoneNumberHash(phoneNumberHash)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        if (!user.getNameKor().equals(name)) {
            throw new CustomException(ErrorCode.USER_NOT_FOUND);
        }

        long endTime = System.nanoTime();
        log.info("[해시검색] 소요시간: {}ms", (endTime - startTime) / 1_000_000);

        return new FindEmailResponseDto(maskEmail(user.getEmail()));
    }

    private String maskEmail(String email) {
        int atIndex = email.indexOf("@");
        if (atIndex <= 2) {
            return email.charAt(0) + "***" + email.substring(atIndex);
        }
        return email.substring(0, 2) + "***" + email.substring(atIndex);
    }
}
