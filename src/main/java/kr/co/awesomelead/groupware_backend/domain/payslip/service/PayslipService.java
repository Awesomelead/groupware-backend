package kr.co.awesomelead.groupware_backend.domain.payslip.service;

import java.io.IOException;
import java.text.Normalizer;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Objects;
import kr.co.awesomelead.groupware_backend.domain.notification.service.NotificationService;
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
import kr.co.awesomelead.groupware_backend.global.infra.s3.service.S3Service;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
public class PayslipService {

    private final PayslipRepository payslipRepository;
    private final PayslipMapper payslipMapper;
    private final UserRepository userRepository;
    private final S3Service s3Service;
    private final NotificationService notificationService;

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
            String normalizedName = Normalizer.normalize(name, Normalizer.Form.NFC);
            String hireDateStr = split[1];
            LocalDate hireDate =
                LocalDate.parse(hireDateStr, DateTimeFormatter.ofPattern("yyyyMMdd"));

            User target =
                userRepository
                    .findByNameAndJoinDate(normalizedName, hireDate)
                    .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

            String s3Key = s3Service.uploadFile(file);

            Payslip savedPayslip = savePayslipInfo(s3Key, originalFileName, target);
            notificationService.sendPayslipAlertToUser(target.getId(), savedPayslip.getId());
        }
    }

    @Transactional
    protected Payslip savePayslipInfo(String s3Key, String originalFileName, User targetUser) {
        Payslip payslip =
            Payslip.builder()
                .fileKey(s3Key)
                .originalFileName(originalFileName)
                .status(PayslipStatus.SENT)
                .user(targetUser)
                .build();
        return payslipRepository.save(payslip);
    }

    // 관리자용 보낸 급여명세서 목록 조회 (Status에 따라)
    @Transactional(readOnly = true)
    public List<AdminPayslipSummaryDto> getPayslipsForAdmin(Long adminId, PayslipStatus status) {
        User admin =
            userRepository
                .findById(adminId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
        if (admin.getRole() != Role.ADMIN && admin.getRole() != Role.MASTER_ADMIN) {
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
        if (admin.getRole() != Role.ADMIN && admin.getRole() != Role.MASTER_ADMIN) {
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
    public List<EmployeePayslipSummaryDto> getPayslips(Long userId) {
        if (!userRepository.existsById(userId)) {
            throw new CustomException(ErrorCode.USER_NOT_FOUND);
        }

        List<Payslip> payslipList = payslipRepository.findAllByUserIdOrderByCreatedAtDesc(userId);

        return payslipMapper.toEmployeePayslipSummaryDtoList(payslipList);
    }

    // 직원의 급여명세서 상세 조회
    @Transactional
    public EmployeePayslipDetailDto getPayslip(Long userId, Long payslipId) {

        Payslip payslip =
            payslipRepository
                .findById(payslipId)
                .orElseThrow(() -> new CustomException(ErrorCode.PAYSLIP_NOT_FOUND));

        if (!payslip.getUser().getId().equals(userId)) {
            throw new CustomException(ErrorCode.NO_AUTHORITY_FOR_VIEW_PAYSLIP);
        }

        // 통보형 프로세스: 직원이 상세 조회하면 열람 완료 처리
        if (payslip.getStatus() == PayslipStatus.SENT) {
            payslip.setStatus(PayslipStatus.READ);
            payslip.setReadAt(LocalDateTime.now());
        }
        return payslipMapper.toEmployeePayslipDetailDto(payslip);
    }

}
