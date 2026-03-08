package kr.co.awesomelead.groupware_backend.test.service;

import kr.co.awesomelead.groupware_backend.domain.auth.dto.response.FindEmailResponseDto;
import kr.co.awesomelead.groupware_backend.domain.department.entity.Department;
import kr.co.awesomelead.groupware_backend.domain.department.enums.Company;
import kr.co.awesomelead.groupware_backend.domain.department.repository.DepartmentRepository;
import kr.co.awesomelead.groupware_backend.domain.user.entity.User;
import kr.co.awesomelead.groupware_backend.domain.user.repository.UserRepository;
import kr.co.awesomelead.groupware_backend.global.error.CustomException;
import kr.co.awesomelead.groupware_backend.global.error.ErrorCode;
import kr.co.awesomelead.groupware_backend.test.dto.request.DummyUsersCreateRequestDto;
import kr.co.awesomelead.groupware_backend.test.dto.response.DummyUsersCreateResponseDto;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class TestService {

    private final UserRepository userRepository;
    private final DepartmentRepository departmentRepository;
    private final BCryptPasswordEncoder passwordEncoder;

    // 테스트용 (인증 없음)

    // 아이디 찾기 - 전체 조회 (인증 우회)
    public FindEmailResponseDto findEmailByAll(String name, String phoneNumber) {
        long startTime = System.nanoTime();

        List<User> users = userRepository.findAllByNameKor(name);
        User user =
                users.stream()
                        .filter(u -> u.getPhoneNumber().equals(phoneNumber))
                        .findFirst()
                        .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        long endTime = System.nanoTime();
        log.info("[전체조회] 조회 {}명, 소요시간: {}ms", users.size(), (endTime - startTime) / 1_000_000);

        return new FindEmailResponseDto(maskEmail(user.getEmail()));
    }

    // 아이디 찾기 - 해시 검색 (인증 우회)
    public FindEmailResponseDto findEmailByHash(String name, String phoneNumber) {
        long startTime = System.nanoTime();

        String phoneNumberHash = User.hashValue(phoneNumber);
        User user =
                userRepository
                        .findByPhoneNumberHash(phoneNumberHash)
                        .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        if (!user.getNameKor().equals(name)) {
            throw new CustomException(ErrorCode.USER_NOT_FOUND);
        }

        long endTime = System.nanoTime();
        log.info("[해시검색] 소요시간: {}ms", (endTime - startTime) / 1_000_000);

        return new FindEmailResponseDto(maskEmail(user.getEmail()));
    }

    private String maskEmail(String email) {
        int atIndex = email.indexOf("@");
        if (atIndex <= 2) {
            return email.charAt(0) + "***" + email.substring(atIndex);
        }
        return email.substring(0, 2) + "***" + email.substring(atIndex);
    }

    public DummyUsersCreateResponseDto createDummyUsers(DummyUsersCreateRequestDto request) {
        List<Department> departments =
                (request.getDepartmentIds() == null || request.getDepartmentIds().isEmpty())
                        ? departmentRepository.findAll().stream().filter(d -> d.getParent() != null).toList()
                        : departmentRepository.findAllById(request.getDepartmentIds());

        if (departments.isEmpty()) {
            throw new CustomException(ErrorCode.DEPARTMENT_NOT_FOUND);
        }

        int requested = request.getCount();
        int created = 0;
        int skipped = 0;
        int seq = request.getStartIndex();
        int deptIndex = 0;

        while (created < requested) {
            Department department = departments.get(deptIndex % departments.size());
            deptIndex++;

            String email = String.format("dummy_user%03d@dummy.local", seq);
            String registrationNumber = String.format("9001013%06d", seq);
            String phoneNumber = String.format("0109%07d", seq);

            boolean duplicated =
                    userRepository.existsByEmail(email)
                            || userRepository.existsByRegistrationNumber(registrationNumber)
                            || userRepository.existsByPhoneNumberHash(User.hashValue(phoneNumber));

            if (duplicated) {
                seq++;
                skipped++;
                if (skipped > requested * 20) {
                    break;
                }
                continue;
            }

            Company workLocation =
                    department.getCompany() != null ? department.getCompany() : Company.AWESOME;

            User user =
                    User.builder()
                            .email(email)
                            .password(passwordEncoder.encode(request.getPassword()))
                            .nameKor(String.format("더미사용자%03d", seq))
                            .nameEng(String.format("DUMMY USER %03d", seq))
                            .nationality("대한민국")
                            .zipcode("06234")
                            .address1("서울특별시 강남구 테헤란로 123")
                            .address2(String.format("더미빌딩 %d층", seq))
                            .registrationNumber(registrationNumber)
                            .phoneNumber(phoneNumber)
                            .hireDate(LocalDate.now())
                            .resignationDate(null)
                            .jobType(request.getJobType())
                            .position(request.getPosition())
                            .role(request.getRole())
                            .status(request.getStatus())
                            .workLocation(workLocation)
                            .department(department)
                            .build();

            userRepository.save(user);
            created++;
            seq++;
        }

        return new DummyUsersCreateResponseDto(requested, created, skipped);
    }
}
