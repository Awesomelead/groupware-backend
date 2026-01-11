package kr.co.awesomelead.groupware_backend.domain.payslip.service;

import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Objects;
import kr.co.awesomelead.groupware_backend.domain.payslip.entity.Payslip;
import kr.co.awesomelead.groupware_backend.domain.payslip.repository.PayslipRepository;
import kr.co.awesomelead.groupware_backend.domain.user.entity.User;
import kr.co.awesomelead.groupware_backend.domain.user.enums.Authority;
import kr.co.awesomelead.groupware_backend.domain.user.repository.UserRepository;
import kr.co.awesomelead.groupware_backend.global.error.CustomException;
import kr.co.awesomelead.groupware_backend.global.error.ErrorCode;
import kr.co.awesomelead.groupware_backend.global.infra.s3.S3Service;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
public class PayslipService {

    private final PayslipRepository payslipRepository;
    private final UserRepository userRepository;
    private final S3Service s3Service;

    @Transactional
    public void sendPayslip(List<MultipartFile> payslipFiles, Long userId) throws IOException {

        User user = userRepository.findById(userId)
            .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
        if (!user.hasAuthority(Authority.MANAGE_EMPLOYEE_DATA)) {
            throw new CustomException(ErrorCode.NO_AUTHORITY_FOR_PAYSLIP);
        }

        for (MultipartFile file : payslipFiles) {

            if (!Objects.equals(file.getContentType(), "application/pdf")) {
                throw new CustomException(ErrorCode.ONLY_PDF_ALLOWED);
            }

            String fileName = file.getOriginalFilename(); // 파일명은 "name_yyyyMMdd_급여명세서.ext" 형식으로 고정된 것으로 가정
            String[] split = fileName.split("_");

            String name = split[0].trim();
            String hireDateStr = split[1];
            LocalDate hireDate = LocalDate.parse(hireDateStr,
                DateTimeFormatter.ofPattern("yyyyMMdd"));

            User target = userRepository.findByNameAndJoinDate(name, hireDate)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

            String s3Key = s3Service.uploadFile(file);

            savePayslipInfo(file, s3Key, target);
        }
    }

    @Transactional
    public void savePayslipInfo(MultipartFile file, String s3Key, User targetUser) {
        Payslip payslip = Payslip.builder()
            .approved(false)
            .fileKey(s3Key)
            .user(targetUser)
            .build();
        payslipRepository.save(payslip);
    }

    // 관리자의 보낸 급여명세서 조회 (거절 및 사유 조회용)

    // 직원의 급여명세서 승인 및 거절

    // 직원의 급여명세서 조회

}
