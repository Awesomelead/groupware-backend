package kr.co.awesomelead.groupware_backend.domain.auth.service;

import java.util.Collection;
import java.util.Iterator;
import kr.co.awesomelead.groupware_backend.domain.aligo.service.PhoneAuthService;
import kr.co.awesomelead.groupware_backend.domain.auth.dto.request.LoginRequestDto;
import kr.co.awesomelead.groupware_backend.domain.auth.dto.request.ResetPasswordByEmailRequestDto;
import kr.co.awesomelead.groupware_backend.domain.auth.dto.request.ResetPasswordByPhoneRequestDto;
import kr.co.awesomelead.groupware_backend.domain.auth.dto.request.ResetPasswordRequestDto;
import kr.co.awesomelead.groupware_backend.domain.auth.dto.request.SignupRequestDto;
import kr.co.awesomelead.groupware_backend.domain.auth.dto.response.AuthTokensDto;
import kr.co.awesomelead.groupware_backend.domain.auth.dto.response.FindEmailResponseDto;
import kr.co.awesomelead.groupware_backend.domain.auth.dto.response.LoginResponseDto;
import kr.co.awesomelead.groupware_backend.domain.auth.dto.response.LoginiResultDto;
import kr.co.awesomelead.groupware_backend.domain.auth.dto.response.SignupResponseDto;
import kr.co.awesomelead.groupware_backend.domain.auth.entity.RefreshToken;
import kr.co.awesomelead.groupware_backend.domain.auth.util.JWTUtil;
import kr.co.awesomelead.groupware_backend.domain.user.entity.User;
import kr.co.awesomelead.groupware_backend.domain.user.mapper.UserMapper;
import kr.co.awesomelead.groupware_backend.domain.user.repository.UserRepository;
import kr.co.awesomelead.groupware_backend.global.error.CustomException;
import kr.co.awesomelead.groupware_backend.global.error.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final BCryptPasswordEncoder bCryptPasswordEncoder;
    private final PhoneAuthService phoneAuthService;
    private final EmailAuthService emailAuthService;
    private final UserMapper userMapper;
    private final AuthenticationManager authenticationManager;
    private final JWTUtil jwtUtil;
    private final RefreshTokenService refreshTokenService;

    @Transactional
    public SignupResponseDto signup(SignupRequestDto joinDto) {

        // 1. 비밀번호 확인 검증
        if (!joinDto.getPassword().equals(joinDto.getPasswordConfirm())) {
            throw new CustomException(ErrorCode.PASSWORD_MISMATCH);
        }

        // 2. 전화번호 인증 여부 확인
        if (!phoneAuthService.isPhoneVerified(joinDto.getPhoneNumber())) {
            throw new CustomException(ErrorCode.PHONE_NOT_VERIFIED);
        }

        // 3. 이메일 인증 여부 확인
        if (!emailAuthService.isEmailVerified(joinDto.getEmail())) {
            throw new CustomException(ErrorCode.EMAIL_NOT_VERIFIED);
        }

        // 4. 이메일 중복 검사
        if (userRepository.existsByEmail(joinDto.getEmail())) {
            throw new CustomException(ErrorCode.DUPLICATE_LOGIN_ID);
        }

        // 5. 주민등록번호 중복 검사
        if (userRepository.existsByRegistrationNumber(joinDto.getRegistrationNumber())) {
            throw new CustomException(ErrorCode.DUPLICATE_REGISTRATION_NUMBER);
        }

        // 6. DTO를 Entity로 변환
        User user = userMapper.toEntity(joinDto);
        // Mapper에서 처리 안 되는 필드만 설정
        user.setPassword(bCryptPasswordEncoder.encode(joinDto.getPassword()));

        // 7. DB에 저장
        User savedUser = userRepository.save(user);

        // 8. 인증 완료 플래그 삭제
        emailAuthService.clearVerification(joinDto.getEmail());
        phoneAuthService.clearVerification(joinDto.getPhoneNumber());

        return new SignupResponseDto(savedUser.getId(), savedUser.getEmail());
    }

    public LoginiResultDto login(LoginRequestDto requestDto) {
        // 1. 인증 처리
        UsernamePasswordAuthenticationToken authToken =
            new UsernamePasswordAuthenticationToken(
                requestDto.getEmail(), requestDto.getPassword(), null);

        Authentication authentication = authenticationManager.authenticate(authToken);

        // 2. 사용자 정보 추출
        String username = authentication.getName();

        // 3. 역할(Role) 정보 추출
        Collection<? extends GrantedAuthority> authorities = authentication.getAuthorities();
        Iterator<? extends GrantedAuthority> iterator = authorities.iterator();
        GrantedAuthority auth = iterator.next();
        String role = auth.getAuthority().replace("ROLE_", "");

        // 4. Access Token 생성 (1시간 유효)
        String accessToken = jwtUtil.createJwt(username, role, 60 * 60 * 1000L);

        // 5. Refresh Token 생성 및 DB 저장
        String refreshToken = refreshTokenService.createAndSaveRefreshToken(username, role);

        // 6. 사용자 정보 조회
        User user =
            userRepository
                .findByEmail(username)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        // 7. 응답 생성
        LoginResponseDto loginResponseDto =
            new LoginResponseDto(
                accessToken, user.getId(), user.getNameKor(), user.getNameEng());

        return new LoginiResultDto(loginResponseDto, refreshToken);
    }

    public void logout(String refreshToken) {
        if (refreshToken != null) {
            refreshTokenService.deleteRefreshToken(refreshToken);
        }
    }

    public AuthTokensDto reissue(String refreshToken) {
        // 1. Refresh Token 검증 및 DB 조회
        RefreshToken storedToken = refreshTokenService.validateRefreshToken(refreshToken);

        // 2. 토큰에서 사용자 정보 추출
        String username = jwtUtil.getUsername(storedToken.getTokenValue());
        String role = jwtUtil.getRole(storedToken.getTokenValue());

        // 3. 새로운 Access Token 생성 (1시간 유효)
        String newAccessToken = jwtUtil.createJwt(username, role, 60 * 60 * 1000L);

        // 4. 새로운 Refresh Token 생성 및 DB 업데이트
        String newRefreshToken = refreshTokenService.createAndSaveRefreshToken(username, role);

        // 5. 두 토큰 모두 반환
        return new AuthTokensDto(newAccessToken, newRefreshToken);
    }

    public FindEmailResponseDto findEmail(String name, String phoneNumber) {
        long startTime = System.currentTimeMillis();

        // 1. 휴대폰 인증 확인
        if (!phoneAuthService.isPhoneVerified(phoneNumber)) {
            throw new CustomException(ErrorCode.PHONE_NOT_VERIFIED);
        }

        // 2. 해시로 사용자 찾기
        String phoneNumberHash = User.hashValue(phoneNumber);
        User user =
            userRepository
                .findByPhoneNumberHash(phoneNumberHash)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        // 3. 이름 검증
        if (!user.getNameKor().equals(name)) {
            throw new CustomException(ErrorCode.USER_NOT_FOUND);
        }

        long endTime = System.currentTimeMillis();
        log.info("해시 검색 방식 소요 시간: {}ms", endTime - startTime);

        // 4. 인증 플래그 삭제
        phoneAuthService.clearVerification(phoneNumber);

        // 5. 응답 생성
        return new FindEmailResponseDto(maskEmail(user.getEmail()));
    }

    private String maskEmail(String email) {
        int atIndex = email.indexOf("@");
        if (atIndex <= 2) {
            return email.charAt(0) + "***" + email.substring(atIndex);
        }
        return email.substring(0, 2) + "***" + email.substring(atIndex);
    }

    public void resetPasswordByEmail(ResetPasswordByEmailRequestDto requestDto) {
        // 1. 이메일 인증 여부 확인
        if (!emailAuthService.isEmailVerified(requestDto.getEmail())) {
            throw new CustomException(ErrorCode.EMAIL_NOT_VERIFIED);
        }
        // 2. 새비밀번호 일치하는지 확인
        if (!requestDto.getNewPassword().equals(requestDto.getNewPasswordConfirm())) {
            throw new CustomException(ErrorCode.PASSWORD_MISMATCH);
        }
        // 3. 이메일로 사용자 찾기
        User user =
            userRepository
                .findByEmail(requestDto.getEmail())
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        // 4. 해당 유저의 비밀번호 변경
        user.setPassword(bCryptPasswordEncoder.encode(requestDto.getNewPassword()));
        userRepository.save(user);

        // 5. 인증 플래그 삭제
        emailAuthService.clearVerification(requestDto.getEmail());

        log.info("비밀번호 재설정 완료 (이메일 인증) - 사용자 ID: {}", user.getId());
    }

    public void resetPasswordByPhone(ResetPasswordByPhoneRequestDto requestDto) {
        // 1. 휴대폰 인증 여부 확인
        if (!phoneAuthService.isPhoneVerified(requestDto.getPhoneNumber())) {
            throw new CustomException(ErrorCode.PHONE_NOT_VERIFIED);
        }
        // 2. 새비밀번호 일치하는지 확인
        if (!requestDto.getNewPassword().equals(requestDto.getNewPasswordConfirm())) {
            throw new CustomException(ErrorCode.PASSWORD_MISMATCH);
        }
        // 3. 해시로 사용자 찾기
        String phoneNumberHash = User.hashValue(requestDto.getPhoneNumber());
        User user =
            userRepository
                .findByPhoneNumberHash(phoneNumberHash)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        // 4. 해당 유저의 비밀번호 변경
        user.setPassword(bCryptPasswordEncoder.encode(requestDto.getNewPassword()));
        userRepository.save(user);

        // 5. 인증 플래그 삭제
        phoneAuthService.clearVerification(requestDto.getPhoneNumber());

        log.info("비밀번호 재설정 완료 (휴대폰 인증) - 사용자 ID: {}", user.getId());
    }

    public void resetPassword(ResetPasswordRequestDto requestDto, Long userId) {
        // 1. 새비밀번호 일치하는지 확인
        if (!requestDto.getNewPassword().equals(requestDto.getNewPasswordConfirm())) {
            throw new CustomException(ErrorCode.PASSWORD_MISMATCH);
        }

        // 2. 사용자 조회
        User user =
            userRepository
                .findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        // 3. 현재 비밀번호 확인
        if (!bCryptPasswordEncoder.matches(requestDto.getCurrentPassword(), user.getPassword())) {
            throw new CustomException(ErrorCode.CURRENT_PASSWORD_MISMATCH);
        }

        // 4. 새 비밀번호가 현재 비밀번호와 같은지 확인
        if (requestDto.getCurrentPassword().equals(requestDto.getNewPassword())) {
            throw new CustomException(ErrorCode.SAME_AS_CURRENT_PASSWORD);
        }

        // 5. 해당 유저의 비밀번호 변경
        user.setPassword(bCryptPasswordEncoder.encode(requestDto.getNewPassword()));
        userRepository.save(user);

        log.info("비밀번호 변경 완료 - 사용자 ID: {}", userId);
    }

    // 계정 삭제 (테스트)
    @Transactional
    public void deleteUser(String email) {
        // 1. 사용자 찾기
        User user =
            userRepository
                .findByEmail(email)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        // 2. 해당 사용자 계정 삭제
        userRepository.delete(user);

        log.info("계정 삭제 완료 - 이메일: {}", email);
    }
}
