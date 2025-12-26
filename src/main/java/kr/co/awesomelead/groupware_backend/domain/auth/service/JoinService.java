package kr.co.awesomelead.groupware_backend.domain.auth.service;

import kr.co.awesomelead.groupware_backend.domain.auth.dto.request.JoinRequestDto;
import kr.co.awesomelead.groupware_backend.domain.aligo.service.PhoneAuthService;
import kr.co.awesomelead.groupware_backend.domain.user.entity.User;
import kr.co.awesomelead.groupware_backend.domain.user.enums.Role;
import kr.co.awesomelead.groupware_backend.domain.user.enums.Status;
import kr.co.awesomelead.groupware_backend.domain.user.repository.UserRepository;
import kr.co.awesomelead.groupware_backend.global.CustomException;
import kr.co.awesomelead.groupware_backend.global.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class JoinService {

    private final UserRepository userRepository;
    private final BCryptPasswordEncoder bCryptPasswordEncoder;
    private final PhoneAuthService phoneAuthService;

    @Transactional
    public void joinProcess(JoinRequestDto joinDto) {

        // 1. 비밀번호 확인 검증
        if (!joinDto.getPassword().equals(joinDto.getPasswordConfirm())) {
            throw new CustomException(ErrorCode.PASSWORD_MISMATCH);
        }

        // 2. 전화번호 인증 여부 확인
        if (!phoneAuthService.isPhoneVerified(joinDto.getPhoneNumber())) {
            throw new CustomException(ErrorCode.PHONE_NOT_VERIFIED);
        }

        // 3. 이메일 중복 검사
        if (userRepository.existsByEmail(joinDto.getEmail())) {
            throw new CustomException(ErrorCode.DUPLICATE_LOGIN_ID);
        }

        // 5. 주민등록번호 중복 검사
        if (userRepository.existsByRegistrationNumber(joinDto.getRegistrationNumber())) {
            throw new CustomException(ErrorCode.DUPLICATE_REGISTRATION_NUMBER);
        }

        // 6. DTO를 Entity로 변환
        User user = new User();
        user.setEmail(joinDto.getEmail());
        user.setPassword(bCryptPasswordEncoder.encode(joinDto.getPassword()));
        user.setNameKor(joinDto.getNameKor());
        user.setNameEng(joinDto.getNameEng());
        user.setNationality(joinDto.getNationality());
        user.setRegistrationNumber(joinDto.getRegistrationNumber());
        user.setPhoneNumber(joinDto.getPhoneNumber());
        user.setWorkLocation(joinDto.getCompany().getDescription());
        // 기본값 설정
        user.setRole(Role.USER);
        user.setStatus(Status.PENDING);

        // 7. DB에 저장
        userRepository.save(user);

        // 8. 인증 완료 플래그 삭제
        phoneAuthService.clearVerification(joinDto.getPhoneNumber());
    }
}
