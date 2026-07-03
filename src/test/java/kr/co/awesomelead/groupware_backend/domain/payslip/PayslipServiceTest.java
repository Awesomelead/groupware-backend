package kr.co.awesomelead.groupware_backend.domain.payslip;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import kr.co.awesomelead.groupware_backend.domain.department.enums.Company;
import kr.co.awesomelead.groupware_backend.domain.notification.service.NotificationService;
import kr.co.awesomelead.groupware_backend.domain.payslip.dto.response.AdminPayslipGroupDto;
import kr.co.awesomelead.groupware_backend.domain.payslip.dto.response.AdminPayslipSummaryDto;
import kr.co.awesomelead.groupware_backend.domain.payslip.dto.response.EmployeePayslipDetailDto;
import kr.co.awesomelead.groupware_backend.domain.payslip.entity.Payslip;
import kr.co.awesomelead.groupware_backend.domain.payslip.enums.PayslipStatus;
import kr.co.awesomelead.groupware_backend.domain.payslip.mapper.PayslipMapper;
import kr.co.awesomelead.groupware_backend.domain.payslip.repository.PayslipRepository;
import kr.co.awesomelead.groupware_backend.domain.payslip.service.PayslipOcrInfoExtractor;
import kr.co.awesomelead.groupware_backend.domain.payslip.service.PayslipPdfInfoExtractor;
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
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.io.IOException;
import java.text.Normalizer;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@ExtendWith(MockitoExtension.class)
public class PayslipServiceTest {

    @InjectMocks private PayslipService payslipService;

    @Mock private PayslipRepository payslipRepository;
    @Mock private UserRepository userRepository;
    @Mock private S3Service s3Service;
    @Mock private PayslipMapper payslipMapper;
    @Mock private NotificationService notificationService;
    @Mock private PayslipPdfInfoExtractor payslipPdfInfoExtractor;
    @Mock private PayslipOcrInfoExtractor payslipOcrInfoExtractor;
    @Mock private BCryptPasswordEncoder bCryptPasswordEncoder;

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
                // admin.hasAuthority(MANAGE_PAYSLIP) 가 false인 상황 (기본값)

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
                admin.getAuthorities().add(Authority.MANAGE_PAYSLIP); // 권한 부여
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
            void it_uploads_to_s3_and_saves_info() throws IOException {
                // given
                admin.getAuthorities().add(Authority.MANAGE_PAYSLIP);
                given(userRepository.findById(1L)).willReturn(Optional.of(admin));

                MockMultipartFile pdfFile =
                        new MockMultipartFile(
                                "payslipFiles",
                                "급여명세서(근로기준1)_10001_홍길동_202605.pdf",
                                "application/pdf",
                                "pdf content".getBytes());

                given(payslipPdfInfoExtractor.extract(pdfFile))
                        .willReturn(
                                new PayslipPdfInfoExtractor.PayslipPersonalInfo(
                                        "홍길동", LocalDate.of(1990, 1, 1)));
                given(userRepository.findAllByNameAndBirthDate("홍길동", LocalDate.of(1990, 1, 1)))
                        .willReturn(List.of(employee));
                given(s3Service.uploadFile(pdfFile)).willReturn("s3-key");
                Payslip savedPayslip = Payslip.builder().id(99L).user(employee).build();
                given(payslipRepository.save(any(Payslip.class))).willReturn(savedPayslip);

                // when
                payslipService.sendPayslip(List.of(pdfFile), 1L);

                // then
                verify(s3Service, times(1)).uploadFile(any());
                verify(payslipRepository, times(1)).save(any(Payslip.class));
                verify(notificationService, times(1)).sendPayslipAlertToUser(employee.getId(), 99L);
            }
        }

        @Nested
        @DisplayName("기본 텍스트 추출 이름이 깨졌지만 OCR로 정상 추출되면")
        class Context_with_corrupted_name_and_ocr_success {

            @Test
            @DisplayName("OCR 결과로 사용자 매칭 후 발송한다.")
            void it_uses_ocr_result_for_matching() throws IOException {
                // given
                admin.getAuthorities().add(Authority.MANAGE_PAYSLIP);
                given(userRepository.findById(1L)).willReturn(Optional.of(admin));

                MockMultipartFile pdfFile =
                        new MockMultipartFile(
                                "payslipFiles",
                                "급여명세서(근로기준1)_10002_리딘뷰_202604.pdf",
                                "application/pdf",
                                "pdf content".getBytes());

                given(payslipPdfInfoExtractor.extract(pdfFile))
                        .willReturn(
                                new PayslipPdfInfoExtractor.PayslipPersonalInfo(
                                        "리", LocalDate.of(1999, 1, 5)));
                given(payslipPdfInfoExtractor.isNameLikelyCorrupted("리")).willReturn(true);
                given(payslipOcrInfoExtractor.isOcrEnabled()).willReturn(true);
                given(payslipOcrInfoExtractor.extract(pdfFile))
                        .willReturn(
                                new PayslipPdfInfoExtractor.PayslipPersonalInfo(
                                        "리딘뷰", LocalDate.of(1999, 1, 5)));
                given(payslipPdfInfoExtractor.isNameLikelyCorrupted("리딘뷰")).willReturn(false);
                given(userRepository.findAllByNameAndBirthDate("리딘뷰", LocalDate.of(1999, 1, 5)))
                        .willReturn(List.of(employee));
                given(s3Service.uploadFile(pdfFile)).willReturn("s3-key");
                Payslip savedPayslip = Payslip.builder().id(100L).user(employee).build();
                given(payslipRepository.save(any(Payslip.class))).willReturn(savedPayslip);

                // when
                payslipService.sendPayslip(List.of(pdfFile), 1L);

                // then
                verify(payslipOcrInfoExtractor, times(1)).extract(pdfFile);
                verify(notificationService, times(1))
                        .sendPayslipAlertToUser(employee.getId(), savedPayslip.getId());
            }
        }

        @Nested
        @DisplayName("이름+생년월일이 같은 사용자가 여러 명이면")
        class Context_with_ambiguous_recipient {

            @Test
            @DisplayName("AMBIGUOUS_PAYSLIP_RECIPIENT 예외를 던진다.")
            void it_throws_ambiguous_recipient_exception() {
                // given
                admin.getAuthorities().add(Authority.MANAGE_PAYSLIP);
                given(userRepository.findById(1L)).willReturn(Optional.of(admin));

                MockMultipartFile pdfFile =
                        new MockMultipartFile(
                                "payslipFiles",
                                "급여명세서(근로기준1)_10001_홍길동_202605.pdf",
                                "application/pdf",
                                "pdf content".getBytes());

                given(payslipPdfInfoExtractor.extract(pdfFile))
                        .willReturn(
                                new PayslipPdfInfoExtractor.PayslipPersonalInfo(
                                        "홍길동", LocalDate.of(1990, 1, 1)));
                given(payslipPdfInfoExtractor.isNameLikelyCorrupted("홍길동")).willReturn(false);

                User duplicate1 = User.builder().id(2L).build();
                User duplicate2 = User.builder().id(3L).build();
                given(userRepository.findAllByNameAndBirthDate("홍길동", LocalDate.of(1990, 1, 1)))
                        .willReturn(List.of(duplicate1, duplicate2));

                // when & then
                assertThatThrownBy(() -> payslipService.sendPayslip(List.of(pdfFile), 1L))
                        .isInstanceOf(CustomException.class)
                        .hasFieldOrPropertyWithValue(
                                "errorCode", ErrorCode.AMBIGUOUS_PAYSLIP_RECIPIENT);
            }
        }

        @Nested
        @DisplayName("파일명 형식이 잘못되면")
        class Context_with_invalid_file_name_format {

            @Test
            @DisplayName("INVALID_PAYSLIP_FILE_NAME_FORMAT 예외를 던진다.")
            void it_throws_invalid_file_name_format_exception() {
                // given
                admin.getAuthorities().add(Authority.MANAGE_PAYSLIP);
                given(userRepository.findById(1L)).willReturn(Optional.of(admin));

                MockMultipartFile pdfFile =
                        new MockMultipartFile(
                                "payslipFiles",
                                "홍길동_19900101_급여명세서.pdf",
                                "application/pdf",
                                "pdf content".getBytes());

                // when & then
                assertThatThrownBy(() -> payslipService.sendPayslip(List.of(pdfFile), 1L))
                        .isInstanceOf(CustomException.class)
                        .hasFieldOrPropertyWithValue(
                                "errorCode", ErrorCode.INVALID_PAYSLIP_FILE_NAME_FORMAT);
            }
        }

        @Nested
        @DisplayName("파일명 이름과 PDF 이름이 다르면")
        class Context_with_file_name_and_pdf_name_mismatch {

            @Test
            @DisplayName("PAYSLIP_USER_INFO_MISMATCH 예외를 던진다.")
            void it_throws_user_info_mismatch_exception() {
                // given
                admin.getAuthorities().add(Authority.MANAGE_PAYSLIP);
                given(userRepository.findById(1L)).willReturn(Optional.of(admin));

                MockMultipartFile pdfFile =
                        new MockMultipartFile(
                                "payslipFiles",
                                "급여명세서(근로기준1)_10001_홍길동_202605.pdf",
                                "application/pdf",
                                "pdf content".getBytes());

                given(payslipPdfInfoExtractor.extract(pdfFile))
                        .willReturn(
                                new PayslipPdfInfoExtractor.PayslipPersonalInfo(
                                        "임꺽정", LocalDate.of(1990, 1, 1)));

                // when & then
                assertThatThrownBy(() -> payslipService.sendPayslip(List.of(pdfFile), 1L))
                        .isInstanceOf(CustomException.class)
                        .hasFieldOrPropertyWithValue(
                                "errorCode", ErrorCode.PAYSLIP_USER_INFO_MISMATCH);
            }
        }
    }

    @Nested
    @DisplayName("updatePayslip 메서드는")
    class Describe_updatePayslip {

        @Test
        @DisplayName("관리자 권한이 없으면 NO_AUTHORITY_FOR_PAYSLIP 예외를 던진다.")
        void it_throws_no_authority_exception() {
            // given
            given(userRepository.findById(1L)).willReturn(Optional.of(admin));
            MockMultipartFile pdfFile =
                    new MockMultipartFile(
                            "payslipFile",
                            "급여명세서(근로기준1)_10001_홍길동_202605.pdf",
                            "application/pdf",
                            "pdf content".getBytes());

            // when & then
            assertThatThrownBy(() -> payslipService.updatePayslip(10L, pdfFile, 1L))
                    .isInstanceOf(CustomException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.NO_AUTHORITY_FOR_PAYSLIP);
        }

        @Test
        @DisplayName("수정 대상 명세서가 없으면 PAYSLIP_NOT_FOUND 예외를 던진다.")
        void it_throws_payslip_not_found_exception() {
            // given
            admin.getAuthorities().add(Authority.MANAGE_PAYSLIP);
            given(userRepository.findById(1L)).willReturn(Optional.of(admin));
            given(payslipRepository.findById(10L)).willReturn(Optional.empty());

            MockMultipartFile pdfFile =
                    new MockMultipartFile(
                            "payslipFile",
                            "급여명세서(근로기준1)_10001_홍길동_202605.pdf",
                            "application/pdf",
                            "pdf content".getBytes());

            // when & then
            assertThatThrownBy(() -> payslipService.updatePayslip(10L, pdfFile, 1L))
                    .isInstanceOf(CustomException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.PAYSLIP_NOT_FOUND);
        }

        @Test
        @DisplayName("유효한 수정 요청이면 파일/수신자/상태를 교체하고 알림을 보낸다.")
        void it_updates_payslip_and_sends_notification() throws IOException {
            // given
            admin.getAuthorities().add(Authority.MANAGE_PAYSLIP);
            given(userRepository.findById(1L)).willReturn(Optional.of(admin));

            User previousOwner = User.builder().id(50L).build();
            Payslip payslip =
                    Payslip.builder()
                            .id(10L)
                            .user(previousOwner)
                            .fileKey("old-key")
                            .originalFileName("급여명세서(근로기준1)_10050_기존직원_202605.pdf")
                            .status(PayslipStatus.READ)
                            .readAt(LocalDateTime.of(2026, 5, 30, 10, 0))
                            .build();
            given(payslipRepository.findById(10L)).willReturn(Optional.of(payslip));

            MockMultipartFile pdfFile =
                    new MockMultipartFile(
                            "payslipFile",
                            "급여명세서(근로기준1)_10001_홍길동_202605.pdf",
                            "application/pdf",
                            "pdf content".getBytes());

            given(payslipPdfInfoExtractor.extract(pdfFile))
                    .willReturn(
                            new PayslipPdfInfoExtractor.PayslipPersonalInfo(
                                    "홍길동", LocalDate.of(1990, 1, 1)));
            given(payslipPdfInfoExtractor.isNameLikelyCorrupted("홍길동")).willReturn(false);
            given(userRepository.findAllByNameAndBirthDate("홍길동", LocalDate.of(1990, 1, 1)))
                    .willReturn(List.of(employee));
            given(s3Service.uploadFile(pdfFile)).willReturn("new-key");

            // when
            payslipService.updatePayslip(10L, pdfFile, 1L);

            // then
            assertThat(payslip.getFileKey()).isEqualTo("new-key");
            assertThat(payslip.getOriginalFileName())
                    .isEqualTo("급여명세서(근로기준1)_10001_홍길동_202605.pdf");
            assertThat(payslip.getUser().getId()).isEqualTo(employee.getId());
            assertThat(payslip.getStatus()).isEqualTo(PayslipStatus.SENT);
            assertThat(payslip.getReadAt()).isNull();

            verify(notificationService).sendPayslipAlertToUser(employee.getId(), 10L);
            verify(s3Service).deleteFile("old-key");
        }
    }

    @Nested
    @DisplayName("deletePayslip 메서드는")
    class Describe_deletePayslip {

        @Test
        @DisplayName("관리자 권한이 없으면 NO_AUTHORITY_FOR_PAYSLIP 예외를 던진다.")
        void it_throws_no_authority_exception() {
            // given
            given(userRepository.findById(1L)).willReturn(Optional.of(admin));

            // when & then
            assertThatThrownBy(() -> payslipService.deletePayslip(10L, 1L))
                    .isInstanceOf(CustomException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.NO_AUTHORITY_FOR_PAYSLIP);
        }

        @Test
        @DisplayName("삭제 대상 명세서가 없으면 PAYSLIP_NOT_FOUND 예외를 던진다.")
        void it_throws_payslip_not_found_exception() {
            // given
            admin.getAuthorities().add(Authority.MANAGE_PAYSLIP);
            given(userRepository.findById(1L)).willReturn(Optional.of(admin));
            given(payslipRepository.findById(10L)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> payslipService.deletePayslip(10L, 1L))
                    .isInstanceOf(CustomException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.PAYSLIP_NOT_FOUND);
        }

        @Test
        @DisplayName("유효한 삭제 요청이면 명세서와 S3 파일을 삭제한다.")
        void it_deletes_payslip_and_s3_file() {
            // given
            admin.getAuthorities().add(Authority.MANAGE_PAYSLIP);
            given(userRepository.findById(1L)).willReturn(Optional.of(admin));

            Payslip payslip = Payslip.builder().id(10L).fileKey("delete-key").build();
            given(payslipRepository.findById(10L)).willReturn(Optional.of(payslip));

            // when
            payslipService.deletePayslip(10L, 1L);

            // then
            verify(payslipRepository).delete(payslip);
            verify(s3Service).deleteFile("delete-key");
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
                assertThatThrownBy(() -> payslipService.getPayslipsForAdmin(1L, null, null))
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
                given(payslipRepository.findAllByStatusAndCompanyOptionalWithUser(null, null))
                        .willReturn(allPayslips);
                given(payslipMapper.toAdminPayslipSummaryDtoList(allPayslips))
                        .willReturn(
                                List.of(
                                        new AdminPayslipSummaryDto(),
                                        new AdminPayslipSummaryDto()));

                // when
                List<AdminPayslipSummaryDto> result =
                        payslipService.getPayslipsForAdmin(1L, null, null);

                // then
                assertThat(result.size()).isEqualTo(2);
                verify(payslipRepository).findAllByStatusAndCompanyOptionalWithUser(null, null);
            }

            @Test
            @DisplayName("회사 조건이 있으면 해당 회사로 필터링해 조회한다.")
            void it_filters_payslips_by_company() {
                // given
                User adminUser = User.builder().id(1L).role(Role.ADMIN).build();
                given(userRepository.findById(1L)).willReturn(Optional.of(adminUser));

                List<Payslip> awesomePayslips = List.of(new Payslip());
                given(
                                payslipRepository.findAllByStatusAndCompanyOptionalWithUser(
                                        PayslipStatus.SENT, Company.AWESOME))
                        .willReturn(awesomePayslips);
                given(payslipMapper.toAdminPayslipSummaryDtoList(awesomePayslips))
                        .willReturn(List.of(new AdminPayslipSummaryDto()));

                // when
                List<AdminPayslipSummaryDto> result =
                        payslipService.getPayslipsForAdmin(1L, PayslipStatus.SENT, Company.AWESOME);

                // then
                assertThat(result.size()).isEqualTo(1);
                verify(payslipRepository)
                        .findAllByStatusAndCompanyOptionalWithUser(
                                PayslipStatus.SENT, Company.AWESOME);
            }
        }

        @Nested
        @DisplayName("월별 그룹 조회를 요청하면")
        class Context_with_grouped_response {

            @Test
            @DisplayName("급여지급월 기준으로 그룹핑해서 최신 월부터 반환한다.")
            void it_returns_grouped_result() {
                // given
                User adminUser = User.builder().id(1L).role(Role.ADMIN).build();
                given(userRepository.findById(1L)).willReturn(Optional.of(adminUser));

                List<Payslip> payslips = List.of(new Payslip(), new Payslip(), new Payslip());
                given(payslipRepository.findAllByStatusAndCompanyOptionalWithUser(null, null))
                        .willReturn(payslips);

                AdminPayslipSummaryDto mayFirst =
                        AdminPayslipSummaryDto.builder()
                                .payslipId(1L)
                                .originalFileName("급여명세서(근로기준1)_10001_홍길동_202605.pdf")
                                .createdAt(LocalDateTime.of(2026, 5, 20, 9, 0))
                                .build();
                AdminPayslipSummaryDto maySecond =
                        AdminPayslipSummaryDto.builder()
                                .payslipId(2L)
                                .originalFileName("급여명세서(근로기준1)_10002_김영희_202605.pdf")
                                .createdAt(LocalDateTime.of(2026, 5, 21, 9, 0))
                                .build();
                AdminPayslipSummaryDto juneOnly =
                        AdminPayslipSummaryDto.builder()
                                .payslipId(3L)
                                .originalFileName("급여명세서(근로기준1)_10003_박민수_202606.pdf")
                                .createdAt(LocalDateTime.of(2026, 6, 1, 9, 0))
                                .build();
                given(payslipMapper.toAdminPayslipSummaryDtoList(payslips))
                        .willReturn(List.of(mayFirst, maySecond, juneOnly));

                // when
                List<AdminPayslipGroupDto> result =
                        payslipService.getPayslipsForAdminGrouped(1L, null, null);

                // then
                assertThat(result.size()).isEqualTo(2);

                AdminPayslipGroupDto firstGroup = result.get(0);
                assertThat(firstGroup.getYearMonth()).isEqualTo("202606");
                assertThat(firstGroup.getTitle()).isEqualTo("2026년 6월");
                assertThat(firstGroup.getTotalCount()).isEqualTo(1);
                assertThat(firstGroup.getItems().get(0).getPayslipId()).isEqualTo(3L);

                AdminPayslipGroupDto secondGroup = result.get(1);
                assertThat(secondGroup.getYearMonth()).isEqualTo("202605");
                assertThat(secondGroup.getTitle()).isEqualTo("2026년 5월");
                assertThat(secondGroup.getTotalCount()).isEqualTo(2);
                // createdAt desc로 정렬되어 최신 건이 먼저
                assertThat(secondGroup.getItems().get(0).getPayslipId()).isEqualTo(2L);
            }

            @Test
            @DisplayName("NFD(분해형 한글) 파일명도 정상적으로 월별 그룹핑한다.")
            void it_groups_nfd_file_name_by_year_month() {
                // given
                User adminUser = User.builder().id(1L).role(Role.ADMIN).build();
                given(userRepository.findById(1L)).willReturn(Optional.of(adminUser));

                List<Payslip> payslips = List.of(new Payslip());
                given(payslipRepository.findAllByStatusAndCompanyOptionalWithUser(null, null))
                        .willReturn(payslips);

                String nfdFileName =
                        Normalizer.normalize(
                                "급여명세서(근로기준1)_10002_리딘뷰_202604.pdf", Normalizer.Form.NFD);
                AdminPayslipSummaryDto summary =
                        AdminPayslipSummaryDto.builder()
                                .payslipId(10L)
                                .originalFileName(nfdFileName)
                                .createdAt(LocalDateTime.of(2026, 4, 30, 9, 0))
                                .build();
                given(payslipMapper.toAdminPayslipSummaryDtoList(payslips))
                        .willReturn(List.of(summary));

                // when
                List<AdminPayslipGroupDto> result =
                        payslipService.getPayslipsForAdminGrouped(1L, null, null);

                // then
                assertThat(result.size()).isEqualTo(1);
                assertThat(result.get(0).getYearMonth()).isEqualTo("202604");
                assertThat(result.get(0).getTitle()).isEqualTo("2026년 4월");
            }

            @Test
            @DisplayName(
                    "originalFileName이 null인 DTO가 있으면 INVALID_PAYSLIP_FILE_NAME_FORMAT 예외를 던진다.")
            void getPayslipsForAdminGrouped_throwsWhenOriginalFileNameIsNull() {
                // given
                User adminUser = User.builder().id(1L).role(Role.ADMIN).build();
                given(userRepository.findById(1L)).willReturn(Optional.of(adminUser));

                List<Payslip> payslips = List.of(new Payslip());
                given(payslipRepository.findAllByStatusAndCompanyOptionalWithUser(null, null))
                        .willReturn(payslips);

                AdminPayslipSummaryDto nullFileName =
                        AdminPayslipSummaryDto.builder()
                                .payslipId(1L)
                                .originalFileName(null)
                                .createdAt(LocalDateTime.of(2026, 5, 20, 9, 0))
                                .build();
                given(payslipMapper.toAdminPayslipSummaryDtoList(payslips))
                        .willReturn(List.of(nullFileName));

                // when & then
                assertThatThrownBy(() -> payslipService.getPayslipsForAdminGrouped(1L, null, null))
                        .isInstanceOf(CustomException.class)
                        .hasFieldOrPropertyWithValue(
                                "errorCode", ErrorCode.INVALID_PAYSLIP_FILE_NAME_FORMAT);
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
                assertThatThrownBy(() -> payslipService.getPayslip(1L, 999L, "password"))
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
                assertThatThrownBy(() -> payslipService.getPayslip(intruderId, 100L, "password"))
                        .isInstanceOf(CustomException.class)
                        .hasFieldOrPropertyWithValue(
                                "errorCode", ErrorCode.NO_AUTHORITY_FOR_VIEW_PAYSLIP);
            }
        }

        @Nested
        @DisplayName("비밀번호가 일치하지 않으면")
        class Context_with_wrong_password {

            @Test
            @DisplayName("PASSWORD_MISMATCH 예외를 던진다.")
            void it_throws_password_mismatch_exception() {
                // given
                User owner = User.builder().id(1L).password("encoded-password").build();
                Payslip myPayslip =
                        Payslip.builder().id(100L).user(owner).status(PayslipStatus.SENT).build();

                given(payslipRepository.findById(100L)).willReturn(Optional.of(myPayslip));
                given(bCryptPasswordEncoder.matches("wrong-password", "encoded-password"))
                        .willReturn(false);

                // when & then
                assertThatThrownBy(() -> payslipService.getPayslip(1L, 100L, "wrong-password"))
                        .isInstanceOf(CustomException.class)
                        .hasFieldOrPropertyWithValue("errorCode", ErrorCode.PASSWORD_MISMATCH);
            }
        }

        @Nested
        @DisplayName("본인의 명세서를 정상적으로 조회하면")
        class Context_with_valid_owner {

            @Test
            @DisplayName("상세 정보 DTO를 반환한다.")
            void it_returns_detail_dto() {
                // given
                User owner = User.builder().id(1L).password("encoded-password").build();
                Payslip myPayslip =
                        Payslip.builder().id(100L).user(owner).status(PayslipStatus.SENT).build();

                given(payslipRepository.findById(100L)).willReturn(Optional.of(myPayslip));
                given(bCryptPasswordEncoder.matches("password123!", "encoded-password"))
                        .willReturn(true);
                given(payslipMapper.toEmployeePayslipDetailDto(myPayslip, s3Service))
                        .willReturn(EmployeePayslipDetailDto.builder().payslipId(100L).build());

                // when
                EmployeePayslipDetailDto result =
                        payslipService.getPayslip(1L, 100L, "password123!");

                // then
                assertThat(result.getPayslipId()).isEqualTo(100L);
                verify(payslipMapper).toEmployeePayslipDetailDto(myPayslip, s3Service);
                assertThat(myPayslip.getStatus()).isEqualTo(PayslipStatus.READ);
                assertThat(myPayslip.getReadAt()).isNotNull();
            }
        }
    }
}
