package kr.co.awesomelead.groupware_backend.domain.admin.service;

import kr.co.awesomelead.groupware_backend.domain.admin.dto.request.AdminUserUpdateRequestDto;
import kr.co.awesomelead.groupware_backend.domain.admin.dto.request.UserApprovalRequestDto;
import kr.co.awesomelead.groupware_backend.domain.admin.dto.response.AdminPendingMyInfoDetailResponseDto;
import kr.co.awesomelead.groupware_backend.domain.admin.dto.response.AdminUserDetailResponseDto;
import kr.co.awesomelead.groupware_backend.domain.admin.dto.response.AdminUserSummaryResponseDto;
import kr.co.awesomelead.groupware_backend.domain.admin.dto.response.MyInfoUpdateRequestSummaryResponseDto;
import kr.co.awesomelead.groupware_backend.domain.admin.dto.response.PendingUserSummaryResponseDto;
import kr.co.awesomelead.groupware_backend.domain.admin.enums.AuthorityAction;
import kr.co.awesomelead.groupware_backend.domain.admin.mapper.AdminMapper;
import kr.co.awesomelead.groupware_backend.domain.aligo.service.PhoneAuthService;
import kr.co.awesomelead.groupware_backend.domain.auth.service.RefreshTokenService;
import kr.co.awesomelead.groupware_backend.domain.department.entity.Department;
import kr.co.awesomelead.groupware_backend.domain.department.enums.Company;
import kr.co.awesomelead.groupware_backend.domain.department.repository.DepartmentRepository;
import kr.co.awesomelead.groupware_backend.domain.notification.enums.NotificationDomainType;
import kr.co.awesomelead.groupware_backend.domain.notification.enums.NotificationMessage;
import kr.co.awesomelead.groupware_backend.domain.notification.service.NotificationService;
import kr.co.awesomelead.groupware_backend.domain.user.entity.MyInfoUpdateRequest;
import kr.co.awesomelead.groupware_backend.domain.user.entity.User;
import kr.co.awesomelead.groupware_backend.domain.user.enums.Authority;
import kr.co.awesomelead.groupware_backend.domain.user.enums.JobType;
import kr.co.awesomelead.groupware_backend.domain.user.enums.MyInfoUpdateRequestStatus;
import kr.co.awesomelead.groupware_backend.domain.user.enums.Position;
import kr.co.awesomelead.groupware_backend.domain.user.enums.Role;
import kr.co.awesomelead.groupware_backend.domain.user.enums.Status;
import kr.co.awesomelead.groupware_backend.domain.user.repository.MyInfoUpdateRequestRepository;
import kr.co.awesomelead.groupware_backend.domain.user.repository.UserRepository;
import kr.co.awesomelead.groupware_backend.domain.user.repository.querydsl.UserQueryRepository;
import kr.co.awesomelead.groupware_backend.global.error.CustomException;
import kr.co.awesomelead.groupware_backend.global.error.ErrorCode;

import lombok.RequiredArgsConstructor;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdminService {

    private final UserRepository userRepository;
    private final UserQueryRepository userQueryRepository;
    private final DepartmentRepository departmentRepository;
    private final MyInfoUpdateRequestRepository myInfoUpdateRequestRepository;
    private final PhoneAuthService phoneAuthService;
    private final RefreshTokenService refreshTokenService;
    private final NotificationService notificationService;
    private final AdminMapper adminMapper;

    @Transactional
    public void approveUserRegistration(
            Long userId, UserApprovalRequestDto requestDto, Long adminId) {
        // 관리자 권한 확인
        User admin =
                userRepository
                        .findById(adminId)
                        .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
        validateRegistrationAuthority(admin);

        // userId로 PENDING 상태의 사용자를 조회
        User user =
                userRepository
                        .findById(userId)
                        .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        if (user.getStatus() != Status.PENDING) {
            throw new CustomException(ErrorCode.DUPLICATED_SIGNUP_REQUEST);
        }

        // 유저가 작성한 값들 중 관리자가 수정하는 값
        if (requestDto.getNameKor() != null) {
            user.setNameKor(requestDto.getNameKor().trim());
        }
        if (requestDto.getNameEng() != null) {
            user.setNameEng(requestDto.getNameEng().trim());
        }

        if (requestDto.getNationality() != null) {
            user.setNationality(requestDto.getNationality());
        }

        if (hasText(requestDto.getZipcode())) {
            user.setZipcode(requestDto.getZipcode().trim());
        }
        if (hasText(requestDto.getAddress1())) {
            user.setAddress1(requestDto.getAddress1().trim());
        }
        if (requestDto.isAddress2Present()) {
            user.setAddress2(
                    hasText(requestDto.getAddress2()) ? requestDto.getAddress2().trim() : null);
        }

        if (hasText(requestDto.getRegistrationNumber())) {
            String newRegNo = requestDto.getRegistrationNumber().trim();
            if (!newRegNo.equals(user.getRegistrationNumber())
                    && userRepository.existsByRegistrationNumber(newRegNo)) {
                throw new CustomException(ErrorCode.DUPLICATE_REGISTRATION_NUMBER);
            }
            user.updateRegistrationNumber(newRegNo);
        }

        if (hasText(requestDto.getPhoneNumber())) {
            String newPhone = requestDto.getPhoneNumber().trim();
            String newPhoneHash = User.hashValue(newPhone);

            if (!newPhoneHash.equals(user.getPhoneNumberHash())) {
                if (!phoneAuthService.isPhoneVerified(newPhone)) {
                    throw new CustomException(ErrorCode.PHONE_NOT_VERIFIED);
                }
                if (userRepository.existsByPhoneNumberHash(newPhoneHash)) {
                    throw new CustomException(ErrorCode.PHONE_NUMBER_ALREADY_EXISTS);
                }
                user.updatePhoneNumber(newPhone);
                phoneAuthService.clearVerification(newPhone);
            }
        }

        Department department =
                departmentRepository
                        .findByName(requestDto.getDepartmentName())
                        .orElseThrow(() -> new CustomException(ErrorCode.DEPARTMENT_NOT_FOUND));

        // DTO의 정보로 사용자 엔티티를 설정
        user.setWorkLocation(requestDto.getWorkLocation());
        user.setDepartment(department);
        user.setJobType(requestDto.getJobType());
        if (requestDto.getRole() != null) {
            user.setRole(requestDto.getRole());
        }
        user.setPosition(requestDto.getPosition());
        user.setHireDate(requestDto.getHireDate());
        // 사용자의 상태를 AVAILABLE로 변경
        user.setStatus(Status.AVAILABLE);

        if (requestDto.getAuthorities() != null) {
            user.getAuthorities().clear();
            requestDto.getAuthorities().forEach(user::addAuthority);
        } else {
            // 관리직의 경우 기본 권한 부여
            if (requestDto.getJobType() == JobType.MANAGEMENT) {
                user.addAuthority(Authority.SEND_NOTIFICATION);
                user.addAuthority(Authority.MANAGE_DEPARTMENT_EDUCATION);
            }
            // ADMIN/MASTER_ADMIN 역할인 경우 모든 권한 부여
            if (requestDto.getRole() == Role.ADMIN || requestDto.getRole() == Role.MASTER_ADMIN) {
                for (Authority authority : Authority.values()) {
                    user.addAuthority(authority);
                }
            }
        }
        userRepository.save(user);

        // 회원가입 승인 처리 완료 시 관리자 승인대기 알림 해제
        notificationService.resolveRequiresApproval(NotificationDomainType.AUTH, user.getId());
    }

    @Transactional
    public void rejectUserRegistration(Long userId, Long adminId) {
        User admin =
                userRepository
                        .findById(adminId)
                        .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
        validateRegistrationAuthority(admin);

        User user =
                userRepository
                        .findById(userId)
                        .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        if (user.getStatus() != Status.PENDING) {
            throw new CustomException(ErrorCode.DUPLICATED_SIGNUP_REQUEST);
        }

        notificationService.resolveRequiresApproval(NotificationDomainType.AUTH, user.getId());
        userRepository.delete(user);
    }

    @Transactional(readOnly = true)
    public List<PendingUserSummaryResponseDto> getPendingSignupUsers(Long adminId) {
        User admin =
                userRepository
                        .findById(adminId)
                        .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
        validateRegistrationAuthority(admin);

        return userRepository.findAllByStatusWithDepartment(Status.PENDING).stream()
                .map(PendingUserSummaryResponseDto::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public Page<AdminUserSummaryResponseDto> getUsers(
            Long adminId,
            String keyword,
            Position position,
            Long departmentId,
            JobType jobType,
            Role role,
            Company workLocation,
            List<Status> statuses,
            Boolean hasPendingMyInfoRequest,
            Pageable pageable) {
        User admin =
                userRepository
                        .findById(adminId)
                        .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
        validateRegistrationAuthority(admin);

        Set<Long> pendingMyInfoUserIds =
                myInfoUpdateRequestRepository
                        .findDistinctUserIdsByStatus(MyInfoUpdateRequestStatus.PENDING)
                        .stream()
                        .collect(Collectors.toSet());

        String normalizedKeyword = hasText(keyword) ? keyword.trim() : null;

        return userQueryRepository
                .findAllForAdminWithFilters(
                        normalizedKeyword,
                        position,
                        departmentId,
                        jobType,
                        role,
                        workLocation,
                        statuses,
                        hasPendingMyInfoRequest,
                        pageable)
                .map(
                        u ->
                                AdminUserSummaryResponseDto.from(
                                        u, pendingMyInfoUserIds.contains(u.getId())));
    }

    @Transactional(readOnly = true)
    public AdminUserDetailResponseDto getUserDetail(Long adminId, Long userId) {
        User admin =
                userRepository
                        .findById(adminId)
                        .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
        validateRegistrationAuthority(admin);

        User user =
                userRepository
                        .findById(userId)
                        .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        boolean hasPendingMyInfoRequest =
                myInfoUpdateRequestRepository.existsByUserIdAndStatus(
                        userId, MyInfoUpdateRequestStatus.PENDING);

        return AdminUserDetailResponseDto.from(user, hasPendingMyInfoRequest);
    }

    @Transactional
    public void updateUserInfo(Long userId, AdminUserUpdateRequestDto requestDto, Long adminId) {
        User admin =
                userRepository
                        .findById(adminId)
                        .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
        validateRegistrationAuthority(admin);

        User user =
                userRepository
                        .findById(userId)
                        .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        if (requestDto.getNameKor() != null) {
            user.setNameKor(requestDto.getNameKor().trim());
        }
        if (requestDto.getNameEng() != null) {
            user.setNameEng(requestDto.getNameEng().trim());
        }
        if (requestDto.getNationality() != null) {
            user.setNationality(requestDto.getNationality());
        }
        if (hasText(requestDto.getZipcode())) {
            user.setZipcode(requestDto.getZipcode().trim());
        }
        if (hasText(requestDto.getAddress1())) {
            user.setAddress1(requestDto.getAddress1().trim());
        }
        if (requestDto.isAddress2Present()) {
            user.setAddress2(
                    hasText(requestDto.getAddress2()) ? requestDto.getAddress2().trim() : null);
        }

        if (hasText(requestDto.getRegistrationNumber())) {
            String newRegNo = requestDto.getRegistrationNumber().trim();
            if (!newRegNo.equals(user.getRegistrationNumber())
                    && userRepository.existsByRegistrationNumber(newRegNo)) {
                throw new CustomException(ErrorCode.DUPLICATE_REGISTRATION_NUMBER);
            }
            user.updateRegistrationNumber(newRegNo);
        }

        if (hasText(requestDto.getPhoneNumber())) {
            String newPhone = requestDto.getPhoneNumber().trim();
            String newPhoneHash = User.hashValue(newPhone);

            if (!newPhoneHash.equals(user.getPhoneNumberHash())) {
                if (!phoneAuthService.isPhoneVerified(newPhone)) {
                    throw new CustomException(ErrorCode.PHONE_NOT_VERIFIED);
                }
                if (userRepository.existsByPhoneNumberHash(newPhoneHash)) {
                    throw new CustomException(ErrorCode.PHONE_NUMBER_ALREADY_EXISTS);
                }
                user.updatePhoneNumber(newPhone);
                phoneAuthService.clearVerification(newPhone);
            }
        }

        if (requestDto.getWorkLocation() != null) {
            user.setWorkLocation(requestDto.getWorkLocation());
        }
        if (requestDto.getDepartmentId() != null) {
            Department department =
                    departmentRepository
                            .findById(requestDto.getDepartmentId())
                            .orElseThrow(() -> new CustomException(ErrorCode.DEPARTMENT_NOT_FOUND));
            user.setDepartment(department);
        }
        if (requestDto.getPosition() != null) {
            user.setPosition(requestDto.getPosition());
        }
        if (requestDto.getJobType() != null) {
            user.setJobType(requestDto.getJobType());
        }
        if (requestDto.getRole() != null) {
            user.setRole(requestDto.getRole());
        }

        if (requestDto.getAuthorities() != null) {
            user.getAuthorities().clear();
            requestDto.getAuthorities().forEach(user::addAuthority);
        }

        if (requestDto.getHireDate() != null) {
            user.setHireDate(requestDto.getHireDate());
        }
        user.updateResignationInfo(requestDto.getResignationDate());
        if (user.getStatus() == Status.SUSPENDED) {
            refreshTokenService.deleteRefreshTokenByEmail(user.getEmail());
        }

        userRepository.save(user);
    }

    @Transactional
    public void updateUserRole(Long userId, Role role, Long adminId) {

        User admin =
                userRepository
                        .findById(adminId)
                        .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        if (admin.getRole() != Role.ADMIN && admin.getRole() != Role.MASTER_ADMIN) {
            throw new CustomException(ErrorCode.NO_AUTHORITY_FOR_ROLE_UPDATE);
        }
        // 1. 대상 사용자 조회
        User targetUser =
                userRepository
                        .findById(userId)
                        .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        // 2. 역할 업데이트
        targetUser.setRole(role);
        targetUser.getAuthorities().clear();

        if (role == Role.MASTER_ADMIN || role == Role.ADMIN) {
            for (Authority authority : Authority.values()) {
                targetUser.addAuthority(authority);
            }
        }

        userRepository.save(targetUser);
    }

    @Transactional
    public void updateUserAuthority(
            Long userId, List<Authority> authorities, AuthorityAction action, Long adminId) {
        // 1. 관리자 권한 확인 (ADMIN 또는 MASTER_ADMIN만 가능)
        User admin =
                userRepository
                        .findById(adminId)
                        .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        if (admin.getRole() != Role.ADMIN && admin.getRole() != Role.MASTER_ADMIN) {
            throw new CustomException(ErrorCode.NO_AUTHORITY_FOR_ROLE_UPDATE);
        }

        // 2. 대상 사용자 조회
        User targetUser =
                userRepository
                        .findById(userId)
                        .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        // 3. 요청 유효성 검사
        if (authorities == null || authorities.isEmpty()) {
            throw new CustomException(ErrorCode.INVALID_ARGUMENT);
        }

        // 4. 동작(Action)에 따른 권한 처리
        switch (action) {
            case ADD -> {
                for (Authority authority : authorities) {
                    if (targetUser.hasAuthority(authority)) {
                        throw new CustomException(ErrorCode.AUTHORITY_ALREADY_ASSIGNED);
                    }
                }
                authorities.forEach(targetUser::addAuthority);
            }
            case REMOVE -> {
                for (Authority authority : authorities) {
                    if (!targetUser.hasAuthority(authority)) {
                        throw new CustomException(ErrorCode.AUTHORITY_NOT_ASSIGNED);
                    }
                }
                authorities.forEach(targetUser::removeAuthority);
            }
        }

        userRepository.save(targetUser);
    }

    @Transactional
    public void approveMyInfoUpdate(Long requestId, Long adminId) {
        User admin =
                userRepository
                        .findById(adminId)
                        .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
        validateMyInfoApprovalAuthority(admin);

        MyInfoUpdateRequest request =
                myInfoUpdateRequestRepository
                        .findById(requestId)
                        .orElseThrow(
                                () ->
                                        new CustomException(
                                                ErrorCode.MY_INFO_UPDATE_REQUEST_NOT_FOUND));

        if (request.getStatus() != MyInfoUpdateRequestStatus.PENDING) {
            throw new CustomException(ErrorCode.MY_INFO_UPDATE_REQUEST_NOT_FOUND);
        }

        User targetUser = request.getUser();

        if (request.getRequestedNameEng() != null) {
            targetUser.setNameEng(request.getRequestedNameEng());
        }
        if (request.getRequestedPhoneNumber() != null) {
            String phoneHash = request.getRequestedPhoneNumberHash();
            if (!phoneHash.equals(targetUser.getPhoneNumberHash())
                    && userRepository.existsByPhoneNumberHash(phoneHash)) {
                throw new CustomException(ErrorCode.PHONE_NUMBER_ALREADY_EXISTS);
            }
            targetUser.updatePhoneNumber(request.getRequestedPhoneNumber());
        }
        if (request.getRequestedZipcode() != null) {
            targetUser.setZipcode(request.getRequestedZipcode());
        }
        if (request.getRequestedAddress1() != null) {
            targetUser.setAddress1(request.getRequestedAddress1());
        }
        if (request.isRequestedAddress2Present()) {
            targetUser.setAddress2(request.getRequestedAddress2());
        }

        request.approve(admin);
        userRepository.save(targetUser);
        myInfoUpdateRequestRepository.save(request);

        // 관리자들의 승인 대기 알림 해제
        notificationService.resolveRequiresApproval(
                NotificationDomainType.MY_INFO_UPDATE, request.getUser().getId());

        // 요청 승인 알림 전송 (FCM + Notification DB)
        notificationService.sendAlertToUser(
                targetUser.getId(),
                NotificationMessage.MY_INFO_UPDATE_APPROVED,
                NotificationDomainType.MY_INFO_UPDATE,
                request.getId(),
                Map.of("requestId", request.getId()));
    }

    @Transactional
    public void rejectMyInfoUpdate(Long requestId, String reason, Long adminId) {
        User admin =
                userRepository
                        .findById(adminId)
                        .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
        validateMyInfoApprovalAuthority(admin);

        if (reason == null || reason.isBlank()) {
            throw new CustomException(ErrorCode.MY_INFO_UPDATE_REJECT_REASON_REQUIRED);
        }

        MyInfoUpdateRequest request =
                myInfoUpdateRequestRepository
                        .findById(requestId)
                        .orElseThrow(
                                () ->
                                        new CustomException(
                                                ErrorCode.MY_INFO_UPDATE_REQUEST_NOT_FOUND));

        if (request.getStatus() != MyInfoUpdateRequestStatus.PENDING) {
            throw new CustomException(ErrorCode.MY_INFO_UPDATE_REQUEST_NOT_FOUND);
        }

        request.reject(admin, reason.trim());
        myInfoUpdateRequestRepository.save(request);

        // 관리자들의 승인 대기 알림 해제
        notificationService.resolveRequiresApproval(
                NotificationDomainType.MY_INFO_UPDATE, request.getUser().getId());

        // 요청 반려 알림 전송 (FCM + Notification DB)
        notificationService.sendAlertToUser(
                request.getUser().getId(),
                NotificationMessage.MY_INFO_UPDATE_REJECTED,
                NotificationDomainType.MY_INFO_UPDATE,
                request.getId(),
                Map.of("requestId", request.getId()),
                reason.trim());
    }

    @Transactional(readOnly = true)
    public List<MyInfoUpdateRequestSummaryResponseDto> getPendingMyInfoUpdateRequests(
            Long adminId) {
        User admin =
                userRepository
                        .findById(adminId)
                        .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
        validateMyInfoApprovalAuthority(admin);

        return myInfoUpdateRequestRepository
                .findAllByStatusWithUser(MyInfoUpdateRequestStatus.PENDING)
                .stream()
                .map(MyInfoUpdateRequestSummaryResponseDto::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public AdminPendingMyInfoDetailResponseDto getPendingMyInfoUpdateRequestDetail(
            Long adminId, Long userId) {
        User admin =
                userRepository
                        .findById(adminId)
                        .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
        validateMyInfoApprovalAuthority(admin);

        userRepository
                .findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        MyInfoUpdateRequest request =
                myInfoUpdateRequestRepository
                        .findFirstByUserIdAndStatusOrderByCreatedAtDesc(
                                userId, MyInfoUpdateRequestStatus.PENDING)
                        .orElseThrow(
                                () ->
                                        new CustomException(
                                                ErrorCode.MY_INFO_UPDATE_REQUEST_NOT_FOUND));

        return adminMapper.toDetailDto(request);
    }

    @Transactional(readOnly = true)
    public byte[] getUsersExcel(
            Long adminId,
            String keyword,
            Position position,
            Long departmentId,
            JobType jobType,
            Role role,
            Company workLocation,
            List<Status> statuses) {
        User admin =
                userRepository
                        .findById(adminId)
                        .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
        validateRegistrationAuthority(admin);

        List<User> users =
                userQueryRepository.findAllForAdminWithFiltersNoPaging(
                        keyword, position, departmentId, jobType, role, workLocation, statuses);

        String[] headers = {
            "No.", "한글 이름", "영문 이름", "생년월일", "국적", "우편번호", "주소1", "주소2", "주민등록번호", "전화번호", "이메일",
            "근무사업장", "부서명", "직급", "근무직종", "입사일", "퇴사일", "역할", "회원가입 상태"
        };

        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("직원명단");

            CellStyle headerStyle = workbook.createCellStyle();
            headerStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerStyle.setFont(headerFont);
            headerStyle.setAlignment(HorizontalAlignment.CENTER);
            setBorderThin(headerStyle);

            CellStyle dataStyle = workbook.createCellStyle();
            setBorderThin(dataStyle);

            Row headerRow = sheet.createRow(0);
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            for (int i = 0; i < users.size(); i++) {
                User u = users.get(i);
                Row row = sheet.createRow(i + 1);
                int col = 0;
                createDataCell(row, col++, String.valueOf(i + 1), dataStyle);
                createDataCell(row, col++, u.getNameKor(), dataStyle);
                createDataCell(row, col++, u.getNameEng(), dataStyle);
                createDataCell(
                        row,
                        col++,
                        u.getBirthDate() != null ? u.getBirthDate().toString() : "",
                        dataStyle);
                createDataCell(row, col++, u.getNationality(), dataStyle);
                createDataCell(row, col++, u.getZipcode(), dataStyle);
                createDataCell(row, col++, u.getAddress1(), dataStyle);
                createDataCell(row, col++, u.getAddress2(), dataStyle);
                createDataCell(row, col++, u.getRegistrationNumber(), dataStyle);
                createDataCell(row, col++, u.getPhoneNumber(), dataStyle);
                createDataCell(row, col++, u.getEmail(), dataStyle);
                createDataCell(
                        row,
                        col++,
                        u.getWorkLocation() != null ? u.getWorkLocation().getDescription() : "",
                        dataStyle);
                createDataCell(
                        row,
                        col++,
                        u.getDepartment() != null
                                ? u.getDepartment().getName().getDescription()
                                : "",
                        dataStyle);
                createDataCell(
                        row,
                        col++,
                        u.getPosition() != null ? u.getPosition().getDescription() : "",
                        dataStyle);
                createDataCell(
                        row,
                        col++,
                        u.getJobType() != null ? u.getJobType().getDescription() : "",
                        dataStyle);
                createDataCell(
                        row,
                        col++,
                        u.getHireDate() != null ? u.getHireDate().toString() : "",
                        dataStyle);
                createDataCell(
                        row,
                        col++,
                        u.getResignationDate() != null ? u.getResignationDate().toString() : "",
                        dataStyle);
                createDataCell(
                        row,
                        col++,
                        u.getRole() != null ? u.getRole().getDescription() : "",
                        dataStyle);
                createDataCell(
                        row,
                        col++,
                        u.getStatus() != null ? u.getStatus().getDescription() : "",
                        dataStyle);
            }

            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
                int currentWidth = sheet.getColumnWidth(i);
                sheet.setColumnWidth(i, Math.max((int) (currentWidth * 1.3), 3000));
            }

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            workbook.write(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException("엑셀 파일 생성 중 오류가 발생했습니다.", e);
        }
    }

    private void setBorderThin(CellStyle style) {
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
    }

    private void createDataCell(Row row, int col, String value, CellStyle style) {
        Cell cell = row.createCell(col);
        cell.setCellValue(value != null ? value : "");
        cell.setCellStyle(style);
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private void validateRegistrationAuthority(User admin) {
        if (admin.getRole() != Role.ADMIN && admin.getRole() != Role.MASTER_ADMIN) {
            throw new CustomException(ErrorCode.NO_AUTHORITY_FOR_REGISTRATION);
        }
    }

    private void validateMyInfoApprovalAuthority(User admin) {
        if (admin.getRole() != Role.ADMIN && admin.getRole() != Role.MASTER_ADMIN) {
            throw new CustomException(ErrorCode.NO_AUTHORITY_FOR_MY_INFO_UPDATE_APPROVAL);
        }
    }
}
