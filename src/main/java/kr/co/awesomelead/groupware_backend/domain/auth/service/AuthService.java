package kr.co.awesomelead.groupware_backend.domain.auth.service;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

import kr.co.awesomelead.groupware_backend.domain.aligo.service.PhoneAuthService;
import kr.co.awesomelead.groupware_backend.domain.approval.entity.Approval;
import kr.co.awesomelead.groupware_backend.domain.approval.entity.ApprovalLineConfig;
import kr.co.awesomelead.groupware_backend.domain.approval.repository.ApprovalLineConfigRepository;
import kr.co.awesomelead.groupware_backend.domain.approval.repository.ApprovalRepository;
import kr.co.awesomelead.groupware_backend.domain.auth.dto.request.BootstrapAdminPromoteRequestDto;
import kr.co.awesomelead.groupware_backend.domain.auth.dto.request.LoginRequestDto;
import kr.co.awesomelead.groupware_backend.domain.auth.dto.request.ResetPasswordByEmailRequestDto;
import kr.co.awesomelead.groupware_backend.domain.auth.dto.request.ResetPasswordByPhoneRequestDto;
import kr.co.awesomelead.groupware_backend.domain.auth.dto.request.ResetPasswordRequestDto;
import kr.co.awesomelead.groupware_backend.domain.auth.dto.request.SignupRequestDto;
import kr.co.awesomelead.groupware_backend.domain.auth.dto.response.AuthTokensDto;
import kr.co.awesomelead.groupware_backend.domain.auth.dto.response.FindEmailResponseDto;
import kr.co.awesomelead.groupware_backend.domain.auth.dto.response.LoginResponseDto;
import kr.co.awesomelead.groupware_backend.domain.auth.dto.response.SignupResponseDto;
import kr.co.awesomelead.groupware_backend.domain.auth.entity.RefreshToken;
import kr.co.awesomelead.groupware_backend.domain.auth.util.JWTUtil;
import kr.co.awesomelead.groupware_backend.domain.notification.enums.NotificationDomainType;
import kr.co.awesomelead.groupware_backend.domain.notification.enums.NotificationMessage;
import kr.co.awesomelead.groupware_backend.domain.notification.service.NotificationService;
import kr.co.awesomelead.groupware_backend.domain.user.dto.response.MyInfoAuthorityItemDto;
import kr.co.awesomelead.groupware_backend.domain.user.entity.User;
import kr.co.awesomelead.groupware_backend.domain.user.enums.Authority;
import kr.co.awesomelead.groupware_backend.domain.user.enums.JobType;
import kr.co.awesomelead.groupware_backend.domain.user.enums.Position;
import kr.co.awesomelead.groupware_backend.domain.user.enums.Role;
import kr.co.awesomelead.groupware_backend.domain.user.enums.Status;
import kr.co.awesomelead.groupware_backend.domain.user.mapper.UserMapper;
import kr.co.awesomelead.groupware_backend.domain.user.repository.UserRepository;
import kr.co.awesomelead.groupware_backend.global.error.CustomException;
import kr.co.awesomelead.groupware_backend.global.error.ErrorCode;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

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
    private final ApprovalRepository approvalRepository;
    private final ApprovalLineConfigRepository approvalLineConfigRepository;
    private final NotificationService notificationService;

    @PersistenceContext private EntityManager entityManager;

    @Value("${spring.jwt.access-validation}")
    private long accessTokenValidation;

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

        // 10. Admin 유저에게 신규 가입 알림 전송 (FCM + Notification DB)
        notificationService.sendAlertToAdmins(
                NotificationMessage.SIGNUP_ADMIN_ALERT,
                NotificationDomainType.AUTH,
                null,
                Map.of("targetId", savedUser.getId()),
                savedUser.getDisplayName());

        return new SignupResponseDto(savedUser.getId(), savedUser.getEmail());
    }

    @Transactional
    public SignupResponseDto bootstrapPromoteAdmin(BootstrapAdminPromoteRequestDto requestDto) {
        if (userRepository.existsByRole(Role.ADMIN)
                || userRepository.existsByRole(Role.MASTER_ADMIN)) {
            throw new CustomException(ErrorCode.BOOTSTRAP_ADMIN_ALREADY_EXISTS);
        }

        User user =
                userRepository
                        .findByEmail(requestDto.getEmail())
                        .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        user.setRole(Role.ADMIN);
        user.setStatus(Status.AVAILABLE);
        if (user.getJobType() == null) {
            user.setJobType(JobType.MANAGEMENT);
        }
        if (user.getPosition() == null) {
            user.setPosition(Position.STAFF);
        }

        for (Authority authority : Authority.values()) {
            user.addAuthority(authority);
        }

        User saved = userRepository.save(user);
        return new SignupResponseDto(saved.getId(), saved.getEmail());
    }

    @Transactional
    public LoginResponseDto login(LoginRequestDto requestDto) {
        // 1. 인증 처리
        UsernamePasswordAuthenticationToken authToken =
                new UsernamePasswordAuthenticationToken(
                        requestDto.getEmail(), requestDto.getPassword(), null);

        Authentication authentication;
        try {
            authentication = authenticationManager.authenticate(authToken);
        } catch (AuthenticationException e) {
            throw new CustomException(ErrorCode.INVALID_LOGIN_CREDENTIALS);
        }

        // 2. 사용자 정보 추출
        String username = authentication.getName();

        // 3. 역할(Role) 정보 추출
        Collection<? extends GrantedAuthority> authorities = authentication.getAuthorities();
        Iterator<? extends GrantedAuthority> iterator = authorities.iterator();
        GrantedAuthority auth = iterator.next();
        String role = auth.getAuthority().replace("ROLE_", "");

        // 4. Access Token 생성
        String accessToken = jwtUtil.createJwt(username, role, accessTokenValidation);

        // 5. Refresh Token 생성 및 DB 저장
        String refreshToken = refreshTokenService.createAndSaveRefreshToken(username, role);

        // 6. 사용자 정보 조회
        User user =
                userRepository
                        .findByEmail(username)
                        .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
        // 7. 응답 생성
        List<MyInfoAuthorityItemDto> authorityDtos =
                user.getAuthorities().stream()
                        .map(
                                authority ->
                                        MyInfoAuthorityItemDto.builder()
                                                .code(authority.name())
                                                .label(authority.getDescription())
                                                .enabled(true)
                                                .build())
                        .toList();

        LoginResponseDto loginResponseDto =
                new LoginResponseDto(
                        accessToken,
                        refreshToken,
                        user.getId(),
                        user.getNameKor(),
                        user.getNameEng(),
                        user.getPosition(),
                        user.getRole(),
                        authorityDtos);

        return loginResponseDto;
    }

    public void logout(String email, String refreshToken) {
        RefreshToken token = refreshTokenService.validateRefreshToken(refreshToken);

        if (!token.getEmail().equals(email)) {
            throw new CustomException(ErrorCode.REFRESH_TOKEN_MISMATCH);
        }

        refreshTokenService.deleteRefreshToken(refreshToken);
    }

    public AuthTokensDto reissue(String refreshToken) {
        // 1. Refresh Token 검증 및 DB 조회
        RefreshToken storedToken = refreshTokenService.validateRefreshToken(refreshToken);

        // 2. 토큰에서 사용자 정보 추출
        String username = jwtUtil.getUsername(storedToken.getTokenValue());
        String role = jwtUtil.getRole(storedToken.getTokenValue());

        // 3. 새로운 Access Token 생성
        String newAccessToken = jwtUtil.createJwt(username, role, accessTokenValidation);

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

    public void verifyAccountByPhone(String email, String phoneNumber) {
        User user =
                userRepository
                        .findByEmail(email)
                        .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        String phoneNumberHash = User.hashValue(phoneNumber);
        if (!phoneNumberHash.equals(user.getPhoneNumberHash())) {
            throw new CustomException(ErrorCode.PHONE_NUMBER_MISMATCH);
        }

        if (!phoneAuthService.isPhoneVerified(phoneNumber)) {
            throw new CustomException(ErrorCode.PHONE_NOT_VERIFIED);
        }
    }

    public void resetPasswordByPhone(ResetPasswordByPhoneRequestDto requestDto) {

        // 1. 이메일로 사용자 조회
        User user =
                userRepository
                        .findByEmail(requestDto.getEmail())
                        .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        String registeredPhoneNumber = user.getPhoneNumber();

        // 2. 등록된 휴대폰 번호 인증 여부 확인
        if (!phoneAuthService.isPhoneVerified(registeredPhoneNumber)) {
            throw new CustomException(ErrorCode.PHONE_NOT_VERIFIED);
        }

        // 3. 새 비밀번호 일치 확인
        if (!requestDto.getNewPassword().equals(requestDto.getNewPasswordConfirm())) {
            throw new CustomException(ErrorCode.PASSWORD_MISMATCH);
        }

        // 4. 비밀번호 변경
        user.setPassword(bCryptPasswordEncoder.encode(requestDto.getNewPassword()));
        userRepository.save(user);

        // 5. 인증 플래그 삭제
        phoneAuthService.clearVerification(registeredPhoneNumber);

        log.info("비밀번호 재설정 완료 (휴대폰 인증) - 사용자 ID: {}, 이메일: {}", user.getId(), user.getEmail());
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

    // 계정 삭제
    @Transactional
    public void deleteUser(Long userId) {
        User user =
                userRepository
                        .findById(userId)
                        .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        // 1) 토큰 및 사용자 직접 참조 데이터 정리
        deleteByQuery(
                "delete from RefreshToken rt where rt.email = :email", "email", user.getEmail());
        deleteByQuery(
                "delete from MyInfoUpdateRequest r where r.reviewedBy.id = :userId",
                "userId",
                userId);
        deleteByQuery(
                "delete from MyInfoUpdateRequest r where r.user.id = :userId", "userId", userId);
        deleteByQuery("delete from RequestHistory rh where rh.user.id = :userId", "userId", userId);
        deleteByQuery("delete from EduAttendance ea where ea.user.id = :userId", "userId", userId);
        deleteByQuery("delete from CheckSheet cs where cs.user.id = :userId", "userId", userId);
        deleteByQuery("delete from Payslip p where p.user.id = :userId", "userId", userId);
        deleteByQuery("delete from AnnualLeave al where al.user.id = :userId", "userId", userId);
        deleteByQuery(
                "delete from VisitRecord vr where vr.visit.user.id = :userId", "userId", userId);
        deleteByQuery("delete from Visit v where v.user.id = :userId", "userId", userId);
        deleteByQuery("delete from NoticeTarget nt where nt.user.id = :userId", "userId", userId);
        deleteByQuery(
                "delete from MessageAttachment ma where ma.message.sender.id = :userId or"
                        + " ma.message.receiver.id = :userId",
                "userId",
                userId);
        deleteByQuery(
                "delete from Message m where m.sender.id = :userId or m.receiver.id = :userId",
                "userId",
                userId);
        deleteByQuery(
                "delete from ApprovalParticipant ap where ap.user.id = :userId", "userId", userId);
        deleteByQuery(
                "delete from ApprovalStep aps where aps.approver.id = :userId", "userId", userId);
        deleteByQuery(
                "delete from NoticeTarget nt where nt.notice.author.id = :userId",
                "userId",
                userId);
        deleteByQuery(
                "delete from NoticeAttachment na where na.notice.author.id = :userId",
                "userId",
                userId);
        deleteByQuery("delete from Notice n where n.author.id = :userId", "userId", userId);

        // 2) 결재선 설정에서 해당 유저 ID 제거
        List<ApprovalLineConfig> configs = approvalLineConfigRepository.findAll();
        for (ApprovalLineConfig config : configs) {
            boolean hasApprover = config.getApproverIds().contains(userId);
            boolean hasReferrer = config.getReferrerIds().contains(userId);
            if (hasApprover || hasReferrer) {
                List<Long> newApprovers =
                        config.getApproverIds().stream().filter(id -> !id.equals(userId)).toList();
                List<Long> newReferrers =
                        config.getReferrerIds().stream().filter(id -> !id.equals(userId)).toList();
                config.update(newApprovers, newReferrers);
            }
        }

        // 3) 사용자가 기안한 결재 문서 삭제 (JOINED 상속 + 자식 테이블 동시 정리)
        List<Approval> draftedApprovals = approvalRepository.findAllByDrafterId(userId);
        if (!draftedApprovals.isEmpty()) {
            approvalRepository.deleteAll(draftedApprovals);
        }

        // 4) 권한 테이블 정리 후 사용자 삭제
        user.getAuthorities().clear();
        userRepository.delete(user);

        log.info("계정 삭제 완료 - userId: {}, email: {}", userId, user.getEmail());
    }

    private void deleteByQuery(String query, String paramName, Object value) {
        entityManager.createQuery(query).setParameter(paramName, value).executeUpdate();
    }
}
