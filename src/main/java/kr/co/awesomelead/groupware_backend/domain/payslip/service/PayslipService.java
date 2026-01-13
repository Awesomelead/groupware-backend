package kr.co.awesomelead.groupware_backend.domain.payslip.service;

import kr.co.awesomelead.groupware_backend.domain.payslip.dto.request.PayslipStatusRequestDto;
import kr.co.awesomelead.groupware_backend.domain.payslip.dto.response.AdminPayslipDetailDto;
import kr.co.awesomelead.groupware_backend.domain.payslip.dto.response.AdminPayslipSummaryDto;
import kr.co.awesomelead.groupware_backend.domain.payslip.dto.response.EmployeePayslipDetailDto;
import kr.co.awesomelead.groupware_backend.domain.payslip.dto.response.EmployeePayslipSummaryDto;
import kr.co.awesomelead.groupware_backend.domain.payslip.entity.Payslip;
import kr.co.awesomelead.groupware_backend.domain.payslip.enums.PayslipStatus;
import kr.co.awesomelead.groupware_backend.domain.payslip.mapper.PayslipMapper;
import kr.co.awesomelead.groupware_backend.domain.payslip.repository.PayslipRepository;
import kr.co.awesomelead.groupware_backend.domain.user.entity.User;
import kr.co.awesomelead.groupware_backend.domain.user.enums.Authority;
import kr.co.awesomelead.groupware_backend.domain.user.enums.Role;
import kr.co.awesomelead.groupware_backend.domain.user.repository.UserRepository;
import kr.co.awesomelead.groupware_backend.global.error.CustomException;
import kr.co.awesomelead.groupware_backend.global.error.ErrorCode;
import kr.co.awesomelead.groupware_backend.global.infra.s3.S3Service;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class PayslipService {

    private final PayslipRepository payslipRepository;
    private final PayslipMapper payslipMapper;
    private final UserRepository userRepository;
    private final S3Service s3Service;

    @Transactional
    public void sendPayslip(List<MultipartFile> payslipFiles, Long userId) throws IOException {

        User user =
                userRepository
                        .findById(userId)
                        .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
        if (!user.hasAuthority(Authority.MANAGE_EMPLOYEE_DATA)) {
            throw new CustomException(ErrorCode.NO_AUTHORITY_FOR_PAYSLIP);
        }

        for (MultipartFile file : payslipFiles) {

            String originalFileName = file.getOriginalFilename();

            if (!Objects.equals(file.getContentType(), "application/pdf")) {
                throw new CustomException(ErrorCode.ONLY_PDF_ALLOWED);
            }

            String fileName =
                    file.getOriginalFilename(); // 파일명은 "name_yyyyMMdd_급여명세서.ext" 형식으로 고정된 것으로 가정
            String[] split = fileName.split("_");

            String name = split[0].trim();
            String hireDateStr = split[1];
            LocalDate hireDate =
                    LocalDate.parse(hireDateStr, DateTimeFormatter.ofPattern("yyyyMMdd"));

            User target =
                    userRepository
                            .findByNameAndJoinDate(name, hireDate)
                            .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

            String s3Key = s3Service.uploadFile(file);

            savePayslipInfo(s3Key, originalFileName, target);
        }
    }

    @Transactional
    protected void savePayslipInfo(String s3Key, String originalFileName, User targetUser) {
        Payslip payslip =
                Payslip.builder()
                        .fileKey(s3Key)
                        .originalFileName(originalFileName)
                        .user(targetUser)
                        .build();
        payslipRepository.save(payslip);
    }

    // 관리자용 보낸 급여명세서 목록 조회 (Status에 따라)
    @Transactional(readOnly = true)
    public List<AdminPayslipSummaryDto> getPayslipsForAdmin(Long adminId, PayslipStatus status) {
        User admin =
                userRepository
                        .findById(adminId)
                        .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
        if (admin.getRole() != Role.ADMIN) {
            throw new CustomException(ErrorCode.NO_AUTHORITY_FOR_PAYSLIP);
        }

        List<Payslip> payslipList = payslipRepository.findAllByStatusOptionalWithUser(status);

        return payslipMapper.toAdminPayslipSummaryDtoList(payslipList);
    }

    // 관리자용 보낸 급여명세서 상세 조회
    @Transactional(readOnly = true)
    public AdminPayslipDetailDto getPayslipForAdmin(Long adminId, Long payslipId) {
        User admin =
                userRepository
                        .findById(adminId)
                        .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
        if (admin.getRole() != Role.ADMIN) {
            throw new CustomException(ErrorCode.NO_AUTHORITY_FOR_PAYSLIP);
        }

        Payslip payslip =
                payslipRepository
                        .findById(payslipId)
                        .orElseThrow(() -> new CustomException(ErrorCode.PAYSLIP_NOT_FOUND));

        return payslipMapper.toAdminPayslipDetailDto(payslip);
    }

    // 직원의 급여명세서 목록 조회 (Status에 따라)
    @Transactional(readOnly = true)
    public List<EmployeePayslipSummaryDto> getPayslips(Long userId, PayslipStatus status) {
        if (!userRepository.existsById(userId)) {
            throw new CustomException(ErrorCode.USER_NOT_FOUND);
        }

        // 리포지토리의 최적화된 메서드 호출
        List<Payslip> payslipList =
                payslipRepository.findAllByUserIdAndStatusOptional(userId, status);

        return payslipMapper.toEmployeePayslipSummaryDtoList(payslipList);
    }

    // 직원의 급여명세서 상세 조회
    @Transactional(readOnly = true)
    public EmployeePayslipDetailDto getPayslip(Long userId, Long payslipId) {

        Payslip payslip =
                payslipRepository
                        .findById(payslipId)
                        .orElseThrow(() -> new CustomException(ErrorCode.PAYSLIP_NOT_FOUND));

        if (!payslip.getUser().getId().equals(userId)) {
            throw new CustomException(ErrorCode.NO_AUTHORITY_FOR_VIEW_PAYSLIP);
        }
        return payslipMapper.toEmployeePayslipDetailDto(payslip);
    }

    // 직원의 급여명세서 승인 및 거절
    @Transactional
    public void respondToPayslip(Long userId, Long payslipId, PayslipStatusRequestDto requestDto) {

        Payslip payslip =
                payslipRepository
                        .findById(payslipId)
                        .orElseThrow(() -> new CustomException(ErrorCode.PAYSLIP_NOT_FOUND));

        if (!payslip.getUser().getId().equals(userId)) {
            throw new CustomException(ErrorCode.NO_AUTHORITY_FOR_VIEW_PAYSLIP);
        }

        if (requestDto.getStatus() == PayslipStatus.REJECTED) {
            validateRejectionReason(requestDto.getRejectionReason()); // 사유 검증 로직 분리
            payslip.setRejectionReason(requestDto.getRejectionReason());
        } else {
            payslip.setRejectionReason(null);
        }

        payslip.setStatus(requestDto.getStatus());
    }

    private void validateRejectionReason(String reason) {
        if (reason == null || reason.isBlank()) {
            throw new CustomException(ErrorCode.NO_REJECTION_REASON_PROVIDED);
        }
    }
}
