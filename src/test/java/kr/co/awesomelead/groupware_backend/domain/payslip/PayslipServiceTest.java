package kr.co.awesomelead.groupware_backend.domain.payslip;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.io.IOException;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import kr.co.awesomelead.groupware_backend.domain.payslip.dto.request.PayslipStatusRequestDto;
import kr.co.awesomelead.groupware_backend.domain.payslip.dto.response.AdminPayslipSummaryDto;
import kr.co.awesomelead.groupware_backend.domain.payslip.dto.response.EmployeePayslipDetailDto;
import kr.co.awesomelead.groupware_backend.domain.payslip.entity.Payslip;
import kr.co.awesomelead.groupware_backend.domain.payslip.enums.PayslipStatus;
import kr.co.awesomelead.groupware_backend.domain.payslip.mapper.PayslipMapper;
import kr.co.awesomelead.groupware_backend.domain.payslip.repository.PayslipRepository;
import kr.co.awesomelead.groupware_backend.domain.payslip.service.PayslipService;
import kr.co.awesomelead.groupware_backend.domain.user.entity.User;
import kr.co.awesomelead.groupware_backend.domain.user.enums.Authority;
import kr.co.awesomelead.groupware_backend.domain.user.enums.Role;
import kr.co.awesomelead.groupware_backend.domain.user.repository.UserRepository;
import kr.co.awesomelead.groupware_backend.global.error.CustomException;
import kr.co.awesomelead.groupware_backend.global.error.ErrorCode;
import kr.co.awesomelead.groupware_backend.global.infra.s3.service.S3Service;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

@ExtendWith(MockitoExtension.class)
public class PayslipServiceTest {

    @InjectMocks
    private PayslipService payslipService;

    @Mock
    private PayslipRepository payslipRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private S3Service s3Service;
    @Mock
    private PayslipMapper payslipMapper;

    private User admin;
    private User employee;

    @BeforeEach
    void setUp() {
        admin = User.builder().id(1L).build();
        employee = User.builder().id(2L).build();
    }

    @Nested
    @DisplayName("sendPayslip 메서드는")
    class Describe_sendPayslip {

        @Nested
        @DisplayName("관리자 권한이 없는 사용자가 요청하면")
        class Context_with_no_authority {

            @Test
            @DisplayName("NO_AUTHORITY_FOR_PAYSLIP 예외를 던진다.")
            void it_throws_no_authority_exception() {
                // given
                given(userRepository.findById(1L)).willReturn(Optional.of(admin));
                // admin.hasAuthority(MANAGE_EMPLOYEE_DATA) 가 false인 상황 (기본값)

                // when & then
                assertThatThrownBy(() -> payslipService.sendPayslip(List.of(), 1L))
                    .isInstanceOf(CustomException.class)
                    .hasFieldOrPropertyWithValue(
                        "errorCode", ErrorCode.NO_AUTHORITY_FOR_PAYSLIP);
            }
        }

        @Nested
        @DisplayName("PDF가 아닌 파일이 포함되어 있으면")
        class Context_with_non_pdf_file {

            @Test
            @DisplayName("ONLY_PDF_ALLOWED 예외를 던진다.")
            void it_throws_only_pdf_exception() {
                // given
                admin.getAuthorities().add(Authority.MANAGE_EMPLOYEE_DATA); // 권한 부여
                given(userRepository.findById(1L)).willReturn(Optional.of(admin));

                MockMultipartFile txtFile =
                    new MockMultipartFile(
                        "payslipFiles", "test.txt", "text/plain", "content".getBytes());

                // when & then
                assertThatThrownBy(() -> payslipService.sendPayslip(List.of(txtFile), 1L))
                    .isInstanceOf(CustomException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.ONLY_PDF_ALLOWED);
            }
        }

        @Nested
        @DisplayName("유효한 PDF 파일과 권한이 있다면")
        class Context_with_valid_request {

            @Test
            @DisplayName("S3에 업로드하고 정보를 저장한다.")
            void it_uploads_to_s3_and_saves_info() throws IOException, IOException {
                // given
                admin.getAuthorities().add(Authority.MANAGE_EMPLOYEE_DATA);
                given(userRepository.findById(1L)).willReturn(Optional.of(admin));

                MockMultipartFile pdfFile =
                    new MockMultipartFile(
                        "payslipFiles",
                        "홍길동_20240101_급여명세서.pdf",
                        "application/pdf",
                        "pdf content".getBytes());

                given(userRepository.findByNameAndJoinDate("홍길동", LocalDate.of(2024, 1, 1)))
                    .willReturn(Optional.of(employee));
                given(s3Service.uploadFile(pdfFile)).willReturn("s3-key");

                // when
                payslipService.sendPayslip(List.of(pdfFile), 1L);

                // then
                verify(s3Service, times(1)).uploadFile(any());
                verify(payslipRepository, times(1)).save(any(Payslip.class));
            }
        }
    }

    @Nested
    @DisplayName("getPayslipsForAdmin 메서드는 (관리자용 목록 조회)")
    class Describe_getPayslipsForAdmin {

        @Nested
        @DisplayName("조회하려는 사용자가 관리자(Role.ADMIN)가 아니라면")
        class Context_with_non_admin_user {

            @Test
            @DisplayName("NO_AUTHORITY_FOR_PAYSLIP 예외를 던진다.")
            void it_throws_no_authority_exception() {
                // given
                User normalUser = User.builder().id(1L).role(Role.USER).build();
                given(userRepository.findById(1L)).willReturn(Optional.of(normalUser));

                // when & then
                assertThatThrownBy(() -> payslipService.getPayslipsForAdmin(1L, null))
                    .isInstanceOf(CustomException.class)
                    .hasFieldOrPropertyWithValue(
                        "errorCode", ErrorCode.NO_AUTHORITY_FOR_PAYSLIP);
            }
        }

        @Nested
        @DisplayName("유효한 관리자가 상태(status) 없이 조회를 요청하면")
        class Context_with_valid_admin_and_no_status {

            @Test
            @DisplayName("전체 명세서 목록을 반환한다.")
            void it_returns_all_payslips() {
                // given
                User adminUser = User.builder().id(1L).role(Role.ADMIN).build();
                given(userRepository.findById(1L)).willReturn(Optional.of(adminUser));

                List<Payslip> allPayslips = List.of(new Payslip(), new Payslip());
                given(payslipRepository.findAllByStatusOptionalWithUser(null))
                    .willReturn(allPayslips);
                given(payslipMapper.toAdminPayslipSummaryDtoList(allPayslips))
                    .willReturn(
                        List.of(
                            new AdminPayslipSummaryDto(),
                            new AdminPayslipSummaryDto()));

                // when
                List<AdminPayslipSummaryDto> result = payslipService.getPayslipsForAdmin(1L, null);

                // then
                assertThat(result.size()).isEqualTo(2);
                verify(payslipRepository).findAllByStatusOptionalWithUser(null);
            }
        }
    }

    @Nested
    @DisplayName("getPayslip (직원용 상세 조회) 메서드는")
    class Describe_getPayslip {

        @Nested
        @DisplayName("존재하지 않는 명세서 ID를 조회하면")
        class Context_with_invalid_payslip_id {

            @Test
            @DisplayName("PAYSLIP_NOT_FOUND 예외를 던진다.")
            void it_throws_not_found_exception() {
                // given
                given(payslipRepository.findById(anyLong())).willReturn(Optional.empty());

                // when & then
                assertThatThrownBy(() -> payslipService.getPayslip(1L, 999L))
                    .isInstanceOf(CustomException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.PAYSLIP_NOT_FOUND);
            }
        }

        @Nested
        @DisplayName("다른 직원의 명세서 상세 정보를 조회하려 하면")
        class Context_accessing_other_users_payslip {

            @Test
            @DisplayName("NO_AUTHORITY_FOR_VIEW_PAYSLIP 예외를 던진다.")
            void it_throws_forbidden_exception() {
                // given
                User owner = User.builder().id(10L).build();
                Payslip otherPayslip = Payslip.builder().id(100L).user(owner).build();

                given(payslipRepository.findById(100L)).willReturn(Optional.of(otherPayslip));
                Long intruderId = 1L; // 침입자 ID

                // when & then
                assertThatThrownBy(() -> payslipService.getPayslip(intruderId, 100L))
                    .isInstanceOf(CustomException.class)
                    .hasFieldOrPropertyWithValue(
                        "errorCode", ErrorCode.NO_AUTHORITY_FOR_VIEW_PAYSLIP);
            }
        }

        @Nested
        @DisplayName("본인의 명세서를 정상적으로 조회하면")
        class Context_with_valid_owner {

            @Test
            @DisplayName("상세 정보 DTO를 반환한다.")
            void it_returns_detail_dto() {
                // given
                User owner = User.builder().id(1L).build();
                Payslip myPayslip = Payslip.builder().id(100L).user(owner).build();

                given(payslipRepository.findById(100L)).willReturn(Optional.of(myPayslip));
                given(payslipMapper.toEmployeePayslipDetailDto(myPayslip))
                    .willReturn(EmployeePayslipDetailDto.builder().payslipId(100L).build());

                // when
                EmployeePayslipDetailDto result = payslipService.getPayslip(1L, 100L);

                // then
                assertThat(result.getPayslipId()).isEqualTo(100L);
                verify(payslipMapper).toEmployeePayslipDetailDto(myPayslip);
            }
        }
    }

    @Nested
    @DisplayName("respondToPayslip 메서드는")
    class Describe_respondToPayslip {

        private Payslip payslip;
        private PayslipStatusRequestDto requestDto;

        @BeforeEach
        void setUp() {
            payslip =
                Payslip.builder().id(100L).user(employee).status(PayslipStatus.PENDING).build();
            requestDto = new PayslipStatusRequestDto();
        }

        @Nested
        @DisplayName("자신의 명세서가 아닌 경우")
        class Context_not_owner {

            @Test
            @DisplayName("NO_AUTHORITY_FOR_VIEW_PAYSLIP 예외를 던진다.")
            void it_throws_forbidden_exception() {
                // given
                given(payslipRepository.findById(100L)).willReturn(Optional.of(payslip));
                Long anotherUserId = 999L;

                // when & then
                assertThatThrownBy(
                    () ->
                        payslipService.respondToPayslip(
                            anotherUserId, 100L, requestDto))
                    .isInstanceOf(CustomException.class)
                    .hasFieldOrPropertyWithValue(
                        "errorCode", ErrorCode.NO_AUTHORITY_FOR_VIEW_PAYSLIP);
            }
        }

        @Nested
        @DisplayName("반려(REJECTED)를 선택했는데 사유가 비어있다면")
        class Context_rejected_without_reason {

            @Test
            @DisplayName("NO_REJECTION_REASON_PROVIDED 예외를 던진다.")
            void it_throws_reason_required_exception() {
                // given
                given(payslipRepository.findById(100L)).willReturn(Optional.of(payslip));
                requestDto.setStatus(PayslipStatus.REJECTED);
                requestDto.setRejectionReason(""); // 빈 사유

                // when & then
                assertThatThrownBy(
                    () ->
                        payslipService.respondToPayslip(
                            employee.getId(), 100L, requestDto))
                    .isInstanceOf(CustomException.class)
                    .hasFieldOrPropertyWithValue(
                        "errorCode", ErrorCode.NO_REJECTION_REASON_PROVIDED);
            }
        }

        @Nested
        @DisplayName("정상적으로 승인(APPROVED)을 하면")
        class Context_approve_success {

            @Test
            @DisplayName("상태가 변경되고 반려 사유는 null이 된다.")
            void it_updates_status_to_approved() {
                // given
                payslip.setRejectionReason("이전 거절 사유");
                given(payslipRepository.findById(100L)).willReturn(Optional.of(payslip));
                requestDto.setStatus(PayslipStatus.APPROVED);

                // when
                payslipService.respondToPayslip(employee.getId(), 100L, requestDto);

                // then
                assertThat(payslip.getStatus()).isEqualTo(PayslipStatus.APPROVED);
                assertThat(payslip.getRejectionReason()).isNull();
            }
        }
    }
}
