package kr.co.awesomelead.groupware_backend.domain.user.service;

import kr.co.awesomelead.groupware_backend.domain.aligo.service.PhoneAuthService;
import kr.co.awesomelead.groupware_backend.domain.user.dto.response.MyInfoResponseDto;
import kr.co.awesomelead.groupware_backend.domain.user.dto.response.UpdateMyInfoRequestDto;
import kr.co.awesomelead.groupware_backend.domain.user.entity.MyInfoUpdateRequest;
import kr.co.awesomelead.groupware_backend.domain.user.entity.User;
import kr.co.awesomelead.groupware_backend.domain.user.enums.MyInfoUpdateRequestStatus;
import kr.co.awesomelead.groupware_backend.domain.user.repository.MyInfoUpdateRequestRepository;
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
    private final MyInfoUpdateRequestRepository myInfoUpdateRequestRepository;
    private final PhoneAuthService phoneAuthService;

    // 내 정보 조회
    @Transactional(readOnly = true)
    public MyInfoResponseDto getMyInfo(UserDetails userDetails) {
        User user =
            userRepository
                .findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        return MyInfoResponseDto.from(user);
    }

    // 내 정보 수정
    @Transactional
    public MyInfoResponseDto updateMyInfo(
        UserDetails userDetails, UpdateMyInfoRequestDto requestDto) {
        User user =
            userRepository
                .findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        if (myInfoUpdateRequestRepository.existsByUserIdAndStatus(
            user.getId(), MyInfoUpdateRequestStatus.PENDING)) {
            throw new CustomException(ErrorCode.MY_INFO_UPDATE_ALREADY_PENDING);
        }

        String requestedNameEng = null;
        String requestedPhoneNumber = null;
        String requestedPhoneNumberHash = null;
        String requestedZipcode = null;
        String requestedAddress1 = null;
        String requestedAddress2 = null;

        if (hasText(requestDto.getNameEng())) {
            requestedNameEng = requestDto.getNameEng().trim();
            if (requestedNameEng.equals(user.getNameEng())) {
                requestedNameEng = null;
            }
        }

        if (hasText(requestDto.getPhoneNumber())) {
            requestedPhoneNumber = requestDto.getPhoneNumber().trim();
            requestedPhoneNumberHash = User.hashValue(requestedPhoneNumber);

            if (requestedPhoneNumberHash.equals(user.getPhoneNumberHash())) {
                throw new CustomException(ErrorCode.PHONE_NUMBER_ALREADY_SAME);
            }
            if (!phoneAuthService.isPhoneVerified(requestedPhoneNumber)) {
                throw new CustomException(ErrorCode.PHONE_NOT_VERIFIED);
            }
            if (userRepository.existsByPhoneNumberHash(requestedPhoneNumberHash)) {
                throw new CustomException(ErrorCode.PHONE_NUMBER_ALREADY_EXISTS);
            }
        }

        if (hasText(requestDto.getZipcode())) {
            requestedZipcode = requestDto.getZipcode().trim();
            if (requestedZipcode.equals(user.getZipcode())) {
                requestedZipcode = null;
            }
        }
        if (hasText(requestDto.getAddress1())) {
            requestedAddress1 = requestDto.getAddress1().trim();
            if (requestedAddress1.equals(user.getAddress1())) {
                requestedAddress1 = null;
            }
        }
        if (hasText(requestDto.getAddress2())) {
            requestedAddress2 = requestDto.getAddress2().trim();
            if (requestedAddress2.equals(user.getAddress2())) {
                requestedAddress2 = null;
            }
        }

        if (requestedNameEng == null
            && requestedPhoneNumber == null
            && requestedZipcode == null
            && requestedAddress1 == null
            && requestedAddress2 == null) {
            throw new CustomException(ErrorCode.MY_INFO_UPDATE_NO_CHANGES);
        }

        MyInfoUpdateRequest request =
            MyInfoUpdateRequest.builder()
                .user(user)
                .requestedNameEng(requestedNameEng)
                .requestedPhoneNumber(requestedPhoneNumber)
                .requestedPhoneNumberHash(requestedPhoneNumberHash)
                .requestedZipcode(requestedZipcode)
                .requestedAddress1(requestedAddress1)
                .requestedAddress2(requestedAddress2)
                .status(MyInfoUpdateRequestStatus.PENDING)
                .build();
        myInfoUpdateRequestRepository.save(request);

        if (requestedPhoneNumber != null) {
            phoneAuthService.clearVerification(requestedPhoneNumber);
        }

        log.info("내 정보 수정 요청 생성 - 사용자 ID: {}, 요청 ID: {}", user.getId(), request.getId());

        return MyInfoResponseDto.from(user);
    }

    @Transactional
    public void cancelMyInfoUpdateRequest(UserDetails userDetails, Long requestId) {
        User user =
            userRepository
                .findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        MyInfoUpdateRequest request =
            myInfoUpdateRequestRepository
                .findById(requestId)
                .orElseThrow(() -> new CustomException(ErrorCode.MY_INFO_UPDATE_REQUEST_NOT_FOUND));

        if (!request.getUser().getId().equals(user.getId())) {
            throw new CustomException(ErrorCode.NO_AUTHORITY_FOR_MY_INFO_UPDATE_CANCEL);
        }

        if (request.getStatus() != MyInfoUpdateRequestStatus.PENDING) {
            throw new CustomException(ErrorCode.MY_INFO_UPDATE_REQUEST_NOT_CANCELABLE);
        }

        request.cancel();
        myInfoUpdateRequestRepository.save(request);
        log.info("내 정보 수정 요청 취소 - 사용자 ID: {}, 요청 ID: {}", user.getId(), requestId);
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
