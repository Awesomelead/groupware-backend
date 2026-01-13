package kr.co.awesomelead.groupware_backend.domain.user.service;

import kr.co.awesomelead.groupware_backend.domain.aligo.service.PhoneAuthService;
import kr.co.awesomelead.groupware_backend.domain.user.dto.response.MyInfoResponseDto;
import kr.co.awesomelead.groupware_backend.domain.user.dto.response.UpdateMyInfoRequestDto;
import kr.co.awesomelead.groupware_backend.domain.user.entity.User;
import kr.co.awesomelead.groupware_backend.domain.user.repository.UserRepository;
import kr.co.awesomelead.groupware_backend.global.error.CustomException;
import kr.co.awesomelead.groupware_backend.global.error.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PhoneAuthService phoneAuthService;

    // 내 정보 조회
    @Transactional(readOnly = true)
    public MyInfoResponseDto getMyInfo(UserDetails userDetails) {
        User user = userRepository.findByEmail(userDetails.getUsername())
            .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        return MyInfoResponseDto.from(user);
    }

    // 내 정보 수정
    @Transactional
    public MyInfoResponseDto updateMyInfo(UserDetails userDetails,
        UpdateMyInfoRequestDto requestDto) {
        // 1. 사용자 조회
        User user = userRepository.findByEmail(userDetails.getUsername())
            .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        boolean isUpdated = false;

        // 2. 영문 이름 수정 (선택 가능)
        if (requestDto.getNameEng() != null && !requestDto.getNameEng().isBlank()) {
            if (requestDto.getNameEng().equals(user.getNameEng())) {
                throw new CustomException(ErrorCode.NAME_ENG_ALREADY_SAME);
            }
            user.setNameEng(requestDto.getNameEng());
            isUpdated = true;
            log.info("영문 이름 수정 - 사용자 ID: {}, 새 이름: {}", user.getId(), requestDto.getNameEng());
        }

        // 3. 전화번호 수정 (선택 가능)
        if (requestDto.getPhoneNumber() != null && !requestDto.getPhoneNumber().isBlank()) {
            String newPhoneHash = User.hashValue(requestDto.getPhoneNumber());

            // 전화번호가 같으면 에러 발생
            if (newPhoneHash.equals(user.getPhoneNumberHash())) {
                throw new CustomException(ErrorCode.PHONE_NUMBER_ALREADY_SAME);
            }

            // 알리고 인증 확인
            if (!phoneAuthService.isPhoneVerified(requestDto.getPhoneNumber())) {
                throw new CustomException(ErrorCode.PHONE_NOT_VERIFIED);
            }

            // 중복 확인
            if (userRepository.existsByPhoneNumberHash(newPhoneHash)) {
                throw new CustomException(ErrorCode.PHONE_NUMBER_ALREADY_EXISTS);
            }

            // 전화번호 업데이트 (해시도 자동 갱신)
            user.updatePhoneNumber(requestDto.getPhoneNumber());

            // 인증 플래그 삭제
            phoneAuthService.clearVerification(requestDto.getPhoneNumber());

            isUpdated = true;
            log.info("전화번호 수정 - 사용자 ID: {}", user.getId());
        }

        // 4. 저장 (변경사항이 있을 때만)
        if (isUpdated) {
            userRepository.save(user);
            log.info("내 정보 수정 완료 - 사용자 ID: {}, 이메일: {}", user.getId(), user.getEmail());
        } else {
            log.info("내 정보 조회 (변경사항 없음) - 사용자 ID: {}", user.getId());
        }

        return MyInfoResponseDto.from(user);
    }
}