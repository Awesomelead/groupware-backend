package kr.co.awesomelead.groupware_backend.domain.user.service;

import kr.co.awesomelead.groupware_backend.domain.user.enums.Role;
import kr.co.awesomelead.groupware_backend.domain.user.enums.Status;
import kr.co.awesomelead.groupware_backend.domain.user.dto.JoinRequestDto;
import kr.co.awesomelead.groupware_backend.domain.user.entity.User;
import kr.co.awesomelead.groupware_backend.domain.user.repository.UserRepository;
import kr.co.awesomelead.groupware_backend.global.CustomException;
import kr.co.awesomelead.groupware_backend.global.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class JoinService {

    private final UserRepository userRepository;
    private final BCryptPasswordEncoder bCryptPasswordEncoder;

    public void joinProcess(JoinRequestDto joinDto) {
        // 1. 로그인 아이디 중복 검사
        if (userRepository.existsByEmail(joinDto.getEmail())) {
            throw new CustomException(ErrorCode.DUPLICATE_LOGIN_ID);
        }

        // 2. DTO를 Entity로 변환
        User user = new User();
        user.setEmail(joinDto.getEmail());
        user.setPassword(bCryptPasswordEncoder.encode(joinDto.getPassword()));

        user.setNameKor(joinDto.getNameKor());
        user.setNameEng(joinDto.getNameEng());
        user.setNationality(joinDto.getNationality());
        user.setRegistrationNumber(joinDto.getRegistrationNumber());
        user.setPhoneNumber(joinDto.getPhoneNumber());

        // 기본값 설정
        user.setRole(Role.ROLE_USER);
        user.setStatus(Status.PENDING);

        // 3. 관리자가 설정할 필드의 기본값 설정
        user.setRole(Role.ROLE_USER);     // 기본 역할: USER
        user.setStatus(Status.PENDING); // 기본 상태: 승인 대기

        // 4. DB에 저장
        userRepository.save(user);
    }
}
