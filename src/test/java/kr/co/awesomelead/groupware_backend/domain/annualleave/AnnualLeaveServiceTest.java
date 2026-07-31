package kr.co.awesomelead.groupware_backend.domain.annualleave;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;

import kr.co.awesomelead.groupware_backend.domain.annualleave.dto.response.AdminAnnualLeaveDispatchDetailResponseDto;
import kr.co.awesomelead.groupware_backend.domain.annualleave.dto.response.AdminAnnualLeaveDispatchGroupResponseDto;
import kr.co.awesomelead.groupware_backend.domain.annualleave.dto.response.AdminAnnualLeaveDispatchItemResponseDto;
import kr.co.awesomelead.groupware_backend.domain.annualleave.dto.response.AnnualLeaveResponseDto;
import kr.co.awesomelead.groupware_backend.domain.annualleave.dto.response.ExcelUploadResponseDto;
import kr.co.awesomelead.groupware_backend.domain.annualleave.entity.AnnualLeave;
import kr.co.awesomelead.groupware_backend.domain.annualleave.entity.AnnualLeaveDispatchHistory;
import kr.co.awesomelead.groupware_backend.domain.annualleave.mapper.AnnualLeaveMapper;
import kr.co.awesomelead.groupware_backend.domain.annualleave.repository.AnnualLeaveDispatchHistoryRepository;
import kr.co.awesomelead.groupware_backend.domain.annualleave.repository.AnnualLeaveRepository;
import kr.co.awesomelead.groupware_backend.domain.annualleave.service.AnnualLeaveService;
import kr.co.awesomelead.groupware_backend.domain.department.enums.Company;
import kr.co.awesomelead.groupware_backend.domain.notification.service.NotificationService;
import kr.co.awesomelead.groupware_backend.domain.user.entity.User;
import kr.co.awesomelead.groupware_backend.domain.user.enums.Authority;
import kr.co.awesomelead.groupware_backend.domain.user.enums.Position;
import kr.co.awesomelead.groupware_backend.domain.user.repository.UserRepository;
import kr.co.awesomelead.groupware_backend.global.error.CustomException;
import kr.co.awesomelead.groupware_backend.global.error.ErrorCode;
import kr.co.awesomelead.groupware_backend.global.infra.s3.service.S3Service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.ClassPathResource;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@ExtendWith(MockitoExtension.class)
public class AnnualLeaveServiceTest {

    @Mock private AnnualLeaveRepository annualLeaveRepository;
    @Mock private AnnualLeaveDispatchHistoryRepository annualLeaveDispatchHistoryRepository;

    @Mock private UserRepository userRepository;

    @Mock private AnnualLeaveMapper annualLeaveMapper;

    @Mock private NotificationService notificationService;
    @Mock private S3Service s3Service;

    @InjectMocks private AnnualLeaveService annualLeaveService;

    @Nested
    @DisplayName("uploadAnnualLeaveFile 메서드는")
    class Describe_uploadAnnualLeaveFile {

        private final Long loginUserId = 1L;
        private final String sheetName = "8월";

        @Nested
        @DisplayName("연차 업로드 권한이 없는 유저가 요청하면")
        class Context_with_no_authority {

            @Test
            @DisplayName("NO_AUTHORITY_FOR_ANNUAL_LEAVE 예외를 던진다")
            void it_throws_exception() {
                // given
                User userWithoutAuth = createMockUser(null); // 권한 없는 유저
                given(userRepository.findById(loginUserId))
                        .willReturn(Optional.of(userWithoutAuth));

                // when & then
                assertThatThrownBy(
                                () ->
                                        annualLeaveService.uploadAnnualLeaveFile(
                                                null, sheetName, loginUserId, Company.AWESOME))
                        .isInstanceOf(CustomException.class)
                        .hasMessageContaining("연차 관리 권한이 없습니다.");
            }
        }

        @Nested
        @DisplayName("정상적인 엑셀 파일이 주어지면")
        class Context_with_valid_excel {

            @Test
            @DisplayName("데이터를 파싱하여 저장하고 성공 결과를 반환한다")
            void it_returns_success_response() throws IOException {
                // given
                User admin = createMockUser(Authority.MANAGE_ANNUAL_LEAVE); // 연차 관리 권한 추가
                given(userRepository.findById(loginUserId)).willReturn(Optional.of(admin));

                MultipartFile mockFile = createMockExcelFileWithRemark("비고 테스트");

                User targetUser =
                        User.builder()
                                .nameKor("테스트 유저")
                                .hireDate(LocalDate.of(2025, 12, 31))
                                .build();
                given(userRepository.findByNameAndJoinDate(anyString(), any()))
                        .willReturn(Optional.of(targetUser));
                given(annualLeaveRepository.findByUser(targetUser)).willReturn(Optional.empty());
                given(s3Service.uploadFile(mockFile)).willReturn("annual-leave/history-1.xlsx");

                // when
                ExcelUploadResponseDto response =
                        annualLeaveService.uploadAnnualLeaveFile(
                                mockFile, sheetName, loginUserId, Company.AWESOME);

                // then
                assertThat(response.getSuccessCount()).isGreaterThan(0);
                assertThat(response.getFailureCount()).isEqualTo(0);
                ArgumentCaptor<AnnualLeave> annualLeaveCaptor =
                        ArgumentCaptor.forClass(AnnualLeave.class);
                verify(annualLeaveRepository, atLeastOnce()).save(annualLeaveCaptor.capture());
                assertThat(annualLeaveCaptor.getValue().getRemark()).isEqualTo("비고 테스트");
                verify(notificationService, atLeastOnce())
                        .sendAnnualLeaveAlertToUser(any(), anyString());
                verify(annualLeaveDispatchHistoryRepository, atLeastOnce()).save(any());
            }
        }

        @Nested
        @DisplayName("엑셀 5행 J열에 날짜 형식이 올바르지 않으면")
        class Context_with_invalid_date_format {

            @Test
            @DisplayName("INVALID_BASE_DATE_FORMAT 예외를 던지며 전체 업로드를 중단한다")
            void it_throws_exception_and_stops_everything() throws IOException {
                // given: 날짜 칸에 "날짜없음" 이라고 적힌 잘못된 엑셀 파일 준비
                MultipartFile invalidFile = createMockExcelFileWithWrongDateFormat();
                User admin = createMockUser(Authority.MANAGE_ANNUAL_LEAVE);
                given(userRepository.findById(loginUserId)).willReturn(Optional.of(admin));

                // when & then
                assertThatThrownBy(
                                () ->
                                        annualLeaveService.uploadAnnualLeaveFile(
                                                invalidFile,
                                                sheetName,
                                                loginUserId,
                                                Company.AWESOME))
                        .isInstanceOf(CustomException.class)
                        .hasMessageContaining(
                                "유효하지 않은 기준일자 형식입니다. (yyyy-MM-dd)"); // ErrorCode 메시지에 따라 수정
            }
        }

        @Nested
        @DisplayName("엑셀 파일에 입력한 시트명이 없으면")
        class Context_with_missing_sheet {

            @Test
            @DisplayName("ANNUAL_LEAVE_SHEET_NOT_FOUND 예외를 던진다")
            void it_throws_when_sheet_not_found() throws IOException {
                // given
                User admin = createMockUser(Authority.MANAGE_ANNUAL_LEAVE);
                given(userRepository.findById(loginUserId)).willReturn(Optional.of(admin));
                MultipartFile mockFile = createMockExcelFile();

                // when & then
                assertThatThrownBy(
                                () ->
                                        annualLeaveService.uploadAnnualLeaveFile(
                                                mockFile,
                                                "6월 1일 연차 기준현황",
                                                loginUserId,
                                                Company.AWESOME))
                        .isInstanceOf(CustomException.class)
                        .hasMessageContaining("입력한 시트명을 엑셀 파일에서 찾을 수 없습니다.");
            }
        }

        @Nested
        @DisplayName("엑셀에 존재하지 않는 직원이 포함되어 있으면")
        class Context_with_unknown_user {

            @Test
            @DisplayName("해당 행은 실패 목록(failures)에 담긴다")
            void it_adds_to_failures() throws IOException {
                // given
                given(userRepository.findById(loginUserId))
                        .willReturn(Optional.of(createMockUser(Authority.MANAGE_ANNUAL_LEAVE)));
                given(userRepository.findByNameAndJoinDate(anyString(), any()))
                        .willReturn(Optional.empty()); // 유저 못 찾음

                MultipartFile mockFile = createMockExcelFile();

                // when
                ExcelUploadResponseDto response =
                        annualLeaveService.uploadAnnualLeaveFile(
                                mockFile, sheetName, loginUserId, Company.AWESOME);

                // then
                assertThat(response.getSuccessCount()).isEqualTo(0);
                assertThat(response.getFailureCount()).isGreaterThan(0);
                assertThat(response.getFailures().get(0).getReason()).contains("직원을 찾을 수 없습니다");
            }
        }

        @Nested
        @DisplayName("파일 읽기 과정에서 문제가 발생하면")
        class Context_with_io_exception {

            @Test
            @DisplayName("FILE_UPLOAD_ERROR 예외를 던진다")
            void it_throws_file_upload_error() throws IOException {
                // given
                MultipartFile mockFile = org.mockito.Mockito.mock(MultipartFile.class);

                User admin = createMockUser(Authority.MANAGE_ANNUAL_LEAVE);
                given(userRepository.findById(loginUserId)).willReturn(Optional.of(admin));

                given(mockFile.getInputStream()).willThrow(new IOException("강제 발생 에러"));

                // when & then
                assertThatThrownBy(
                                () ->
                                        annualLeaveService.uploadAnnualLeaveFile(
                                                mockFile, sheetName, loginUserId, Company.AWESOME))
                        .isInstanceOf(CustomException.class)
                        .hasMessageContaining("파일 업로드 중 오류가 발생했습니다."); // ErrorCode의 메시지에 맞춰 수정
            }
        }
    }

    @Nested
    @DisplayName("getAnnualLeave 메서드는")
    class Describe_getAnnualLeave {

        @Nested
        @DisplayName("유효한 유저로 요청하면")
        class Context_with_valid_user {

            @Test
            @DisplayName("정상적으로 연차 정보를 반환한다")
            void it_returns_annual_leave_info() {
                // given
                AnnualLeave annualLeave =
                        AnnualLeave.builder()
                                .id(1L)
                                .total(15.0)
                                .used(5.0)
                                .remain(10.0)
                                .updateDate(LocalDate.now())
                                .build();
                Long userId = 1L;
                User user =
                        User.builder()
                                .id(userId)
                                .nameKor("테스트 유저")
                                .annualLeave(annualLeave)
                                .build();
                AnnualLeaveResponseDto annualLeaveResponseDto =
                        AnnualLeaveResponseDto.builder().total(15.0).used(5.0).remain(10.0).build();
                given(userRepository.findById(userId)).willReturn(Optional.of(user));
                given(annualLeaveMapper.toAnnualLeaveResponseDto(annualLeave))
                        .willReturn(annualLeaveResponseDto);

                // when
                AnnualLeaveResponseDto result = annualLeaveService.getAnnualLeave(userId);

                // then
                assertThat(result).isNotNull();
                assertThat(result.getTotal()).isEqualTo(15.0);
                assertThat(result.getUsed()).isEqualTo(5.0);
                verify(annualLeaveMapper, atLeastOnce()).toAnnualLeaveResponseDto(any());
            }
        }

        @Nested
        @DisplayName("존재하지 않는 유저로 요청하면")
        class Context_with_invalid_user {

            @Test
            @DisplayName("USER_NOT_FOUND 예외를 던진다")
            void it_throws_exception() {
                // given
                Long userId = 999L;
                given(userRepository.findById(userId)).willReturn(Optional.empty());

                // when & then
                assertThatThrownBy(() -> annualLeaveService.getAnnualLeave(userId))
                        .isInstanceOf(CustomException.class)
                        .hasMessageContaining("사용자를 찾을 수 없습니다.");
            }
        }
    }

    @Nested
    @DisplayName("getAnnualLeavesForAdmin 메서드는")
    class Describe_getAnnualLeavesForAdmin {

        @Test
        @DisplayName("연차 관리 권한이 없는 유저가 요청하면 NO_AUTHORITY_FOR_ANNUAL_LEAVE 예외를 던진다")
        void it_throws_when_user_has_no_authority() {
            // given
            Long userId = 1L;
            given(userRepository.findById(userId)).willReturn(Optional.of(createMockUser(null)));

            // when & then
            assertThatThrownBy(() -> annualLeaveService.getAnnualLeavesForAdmin(userId, null))
                    .isInstanceOf(CustomException.class)
                    .hasMessageContaining("연차 관리 권한이 없습니다.");
        }

        @Test
        @DisplayName("권한이 있는 관리자가 요청하면 연도 그룹과 월 제목으로 발송 목록을 반환한다")
        void it_returns_dispatched_annual_leave_list() {
            // given
            Long adminId = 1L;
            User admin = createMockUser(Authority.MANAGE_ANNUAL_LEAVE);
            admin.setPosition(Position.MANAGER);
            given(userRepository.findById(adminId)).willReturn(Optional.of(admin));

            AnnualLeaveDispatchHistory first =
                    AnnualLeaveDispatchHistory.builder()
                            .id(10L)
                            .uploadedBy(admin)
                            .originalFileName("2026_연차현황.xlsx")
                            .sheetName("2026-06")
                            .fileKey("annual-leave/2026-06-first.xlsx")
                            .baseDate(LocalDate.of(2026, 6, 1))
                            .createdAt(LocalDateTime.of(2026, 6, 1, 9, 0))
                            .company(Company.AWESOME)
                            .build();
            AnnualLeaveDispatchHistory second =
                    AnnualLeaveDispatchHistory.builder()
                            .id(11L)
                            .uploadedBy(admin)
                            .originalFileName("2026_연차현황_수정본.xlsx")
                            .sheetName("2026-06")
                            .fileKey("annual-leave/2026-06-second.xlsx")
                            .baseDate(LocalDate.of(2026, 6, 1))
                            .createdAt(LocalDateTime.of(2026, 6, 2, 9, 0))
                            .company(Company.AWESOME)
                            .build();
            AnnualLeaveDispatchHistory third =
                    AnnualLeaveDispatchHistory.builder()
                            .id(12L)
                            .uploadedBy(admin)
                            .originalFileName("2026_연차현황_7월.xlsx")
                            .sheetName("2026-07")
                            .fileKey("annual-leave/2026-07-first.xlsx")
                            .baseDate(LocalDate.of(2026, 7, 1))
                            .createdAt(LocalDateTime.of(2026, 7, 1, 9, 0))
                            .company(Company.AWESOME)
                            .build();
            given(annualLeaveDispatchHistoryRepository.findAllByCompanyOrderByCreatedAtDesc(null))
                    .willReturn(List.of(first, second, third));
            given(
                            s3Service.getPresignedDownloadUrl(
                                    "annual-leave/2026-06-first.xlsx", "2026_연차현황.xlsx"))
                    .willReturn("https://example.com/annual-leave-2026-06-first");
            given(
                            s3Service.getPresignedDownloadUrl(
                                    "annual-leave/2026-06-second.xlsx", "2026_연차현황_수정본.xlsx"))
                    .willReturn("https://example.com/annual-leave-2026-06-second");
            given(
                            s3Service.getPresignedDownloadUrl(
                                    "annual-leave/2026-07-first.xlsx", "2026_연차현황_7월.xlsx"))
                    .willReturn("https://example.com/annual-leave-2026-07-first");

            // when
            List<AdminAnnualLeaveDispatchGroupResponseDto> result =
                    annualLeaveService.getAnnualLeavesForAdmin(adminId, null);

            // then
            assertThat(result.size()).isEqualTo(1);
            assertThat(result.get(0).getSheetName()).isEqualTo("2026년");
            assertThat(result.get(0).getTitle()).isEqualTo("2026년");
            assertThat(result.get(0).getTotalCount()).isEqualTo(3);
            assertThat(result.get(0).getItems().get(0).getDispatchId()).isEqualTo(10L);
            assertThat(result.get(0).getItems().get(0).getTitle()).isEqualTo("6월");
            assertThat(result.get(0).getItems().get(0).getOriginalFileName())
                    .isEqualTo("2026_연차현황.xlsx");
            assertThat(result.get(0).getItems().get(0).getSheetName()).isEqualTo("2026-06");
            assertThat(result.get(0).getItems().get(0).getCompany()).isEqualTo(Company.AWESOME);
            assertThat(result.get(0).getItems().get(0).getCreatedAt())
                    .isEqualTo(LocalDateTime.of(2026, 6, 1, 9, 0));
            assertThat(result.get(0).getItems().get(0).getSenderName()).isEqualTo("테스트 업로더");
            assertThat(result.get(0).getItems().get(0).getSenderPosition()).isEqualTo("과장");
            assertThat(result.get(0).getItems().get(1).getTitle()).isEqualTo("6월");
            assertThat(result.get(0).getItems().get(1).getOriginalFileName())
                    .isEqualTo("2026_연차현황_수정본.xlsx");
            assertThat(result.get(0).getItems().get(0).getFileUrl())
                    .isEqualTo("https://example.com/annual-leave-2026-06-first");
            assertThat(result.get(0).getItems().get(2).getTitle()).isEqualTo("7월");
            assertThat(result.get(0).getItems().get(2).getFileUrl())
                    .isEqualTo("https://example.com/annual-leave-2026-07-first");
        }
    }

    @Nested
    @DisplayName("updateAnnualLeaveDispatchForAdmin 메서드는")
    class Describe_updateAnnualLeaveDispatchForAdmin {

        @Test
        @DisplayName("연차 관리 권한이 없는 유저가 요청하면 NO_AUTHORITY_FOR_ANNUAL_LEAVE 예외를 던진다")
        void it_throws_when_user_has_no_authority() throws IOException {
            // given
            Long userId = 1L;
            given(userRepository.findById(userId)).willReturn(Optional.of(createMockUser(null)));
            MultipartFile mockFile = createMockExcelFile();

            // when & then
            assertThatThrownBy(
                            () ->
                                    annualLeaveService.updateAnnualLeaveDispatchForAdmin(
                                            userId, 10L, mockFile, "8월", Company.AWESOME))
                    .isInstanceOf(CustomException.class)
                    .hasMessageContaining("연차 관리 권한이 없습니다.");
        }

        @Test
        @DisplayName("해당 발송 이력이 없으면 ANNUAL_LEAVE_DISPATCH_HISTORY_NOT_FOUND 예외를 던진다")
        void it_throws_when_history_not_found() throws IOException {
            // given
            Long adminId = 1L;
            User admin = createMockUser(Authority.MANAGE_ANNUAL_LEAVE);
            given(userRepository.findById(adminId)).willReturn(Optional.of(admin));
            given(annualLeaveDispatchHistoryRepository.findById(999L)).willReturn(Optional.empty());
            MultipartFile mockFile = createMockExcelFile();

            // when & then
            assertThatThrownBy(
                            () ->
                                    annualLeaveService.updateAnnualLeaveDispatchForAdmin(
                                            adminId, 999L, mockFile, "8월", Company.AWESOME))
                    .isInstanceOf(CustomException.class)
                    .hasMessageContaining("해당 연차 발송 이력을 찾을 수 없습니다.");
        }

        @Test
        @DisplayName("권한이 있는 관리자가 요청하면 발송 이력을 수정하고 기존 파일을 삭제한다")
        void it_updates_dispatch_history() throws IOException {
            // given
            Long adminId = 1L;
            Long dispatchId = 10L;
            User admin = createMockUser(Authority.MANAGE_ANNUAL_LEAVE);
            given(userRepository.findById(adminId)).willReturn(Optional.of(admin));

            AnnualLeaveDispatchHistory history =
                    AnnualLeaveDispatchHistory.builder()
                            .id(dispatchId)
                            .uploadedBy(admin)
                            .originalFileName("old.xlsx")
                            .sheetName("8월")
                            .fileKey("annual-leave/old.xlsx")
                            .baseDate(LocalDate.of(2026, 8, 1))
                            .company(Company.AWESOME)
                            .build();
            given(annualLeaveDispatchHistoryRepository.findById(dispatchId))
                    .willReturn(Optional.of(history));

            MultipartFile mockFile = createMockExcelFile();
            User targetUser =
                    User.builder().nameKor("테스트 유저").hireDate(LocalDate.of(2025, 12, 31)).build();
            given(userRepository.findByNameAndJoinDate(anyString(), any()))
                    .willReturn(Optional.of(targetUser));
            given(annualLeaveRepository.findByUser(targetUser)).willReturn(Optional.empty());
            given(s3Service.uploadFile(mockFile)).willReturn("annual-leave/new.xlsx");

            // when
            ExcelUploadResponseDto result =
                    annualLeaveService.updateAnnualLeaveDispatchForAdmin(
                            adminId, dispatchId, mockFile, "8월", Company.AWESOME);

            // then
            assertThat(result.getSuccessCount()).isGreaterThan(0);
            assertThat(history.getOriginalFileName()).isEqualTo("annual_leave_test.xlsx");
            assertThat(history.getSheetName()).isEqualTo("8월");
            assertThat(history.getFileKey()).isEqualTo("annual-leave/new.xlsx");
            verify(s3Service).deleteFile("annual-leave/old.xlsx");
        }
    }

    @Nested
    @DisplayName("deleteAnnualLeaveDispatchForAdmin 메서드는")
    class Describe_deleteAnnualLeaveDispatchForAdmin {

        @Test
        @DisplayName("연차 관리 권한이 없는 유저가 요청하면 NO_AUTHORITY_FOR_ANNUAL_LEAVE 예외를 던진다")
        void it_throws_when_user_has_no_authority() {
            // given
            Long userId = 1L;
            given(userRepository.findById(userId)).willReturn(Optional.of(createMockUser(null)));

            // when & then
            assertThatThrownBy(
                            () -> annualLeaveService.deleteAnnualLeaveDispatchForAdmin(userId, 10L))
                    .isInstanceOf(CustomException.class)
                    .hasMessageContaining("연차 관리 권한이 없습니다.");
        }

        @Test
        @DisplayName("해당 발송 이력이 없으면 ANNUAL_LEAVE_DISPATCH_HISTORY_NOT_FOUND 예외를 던진다")
        void it_throws_when_history_not_found() {
            // given
            Long adminId = 1L;
            User admin = createMockUser(Authority.MANAGE_ANNUAL_LEAVE);
            given(userRepository.findById(adminId)).willReturn(Optional.of(admin));
            given(annualLeaveDispatchHistoryRepository.findById(999L)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(
                            () ->
                                    annualLeaveService.deleteAnnualLeaveDispatchForAdmin(
                                            adminId, 999L))
                    .isInstanceOf(CustomException.class)
                    .hasMessageContaining("해당 연차 발송 이력을 찾을 수 없습니다.");
        }

        @Test
        @DisplayName("권한이 있는 관리자가 요청하면 발송 이력과 연결 파일을 삭제한다")
        void it_deletes_dispatch_history() {
            // given
            Long adminId = 1L;
            Long dispatchId = 10L;
            User admin = createMockUser(Authority.MANAGE_ANNUAL_LEAVE);
            given(userRepository.findById(adminId)).willReturn(Optional.of(admin));

            AnnualLeaveDispatchHistory history =
                    AnnualLeaveDispatchHistory.builder()
                            .id(dispatchId)
                            .uploadedBy(admin)
                            .originalFileName("2026_연차현황.xlsx")
                            .sheetName("8월")
                            .fileKey("annual-leave/2026-08.xlsx")
                            .build();
            given(annualLeaveDispatchHistoryRepository.findById(dispatchId))
                    .willReturn(Optional.of(history));
            given(annualLeaveDispatchHistoryRepository.findAll()).willReturn(List.of());

            // when
            annualLeaveService.deleteAnnualLeaveDispatchForAdmin(adminId, dispatchId);

            // then
            verify(annualLeaveDispatchHistoryRepository).delete(history);
            verify(annualLeaveRepository).deleteAllInBatch();
            verify(s3Service).deleteFile("annual-leave/2026-08.xlsx");
        }

        @Test
        @DisplayName("삭제 후 남은 발송 이력의 원본 파일 기준으로 연차 정보를 다시 계산한다")
        void it_rebuilds_annual_leaves_from_remaining_dispatch_histories() throws IOException {
            // given
            Long adminId = 1L;
            Long dispatchId = 10L;
            User admin = createMockUser(Authority.MANAGE_ANNUAL_LEAVE);
            given(userRepository.findById(adminId)).willReturn(Optional.of(admin));

            AnnualLeaveDispatchHistory deletedHistory =
                    AnnualLeaveDispatchHistory.builder()
                            .id(dispatchId)
                            .uploadedBy(admin)
                            .originalFileName("2026_연차현황_9월.xlsx")
                            .sheetName("9월")
                            .fileKey("annual-leave/2026-09.xlsx")
                            .baseDate(LocalDate.of(2026, 9, 1))
                            .build();
            AnnualLeaveDispatchHistory remainingHistory =
                    AnnualLeaveDispatchHistory.builder()
                            .id(9L)
                            .uploadedBy(admin)
                            .originalFileName("2026_연차현황_8월.xlsx")
                            .sheetName("8월")
                            .fileKey("annual-leave/2026-08.xlsx")
                            .baseDate(LocalDate.of(2026, 8, 1))
                            .build();
            given(annualLeaveDispatchHistoryRepository.findById(dispatchId))
                    .willReturn(Optional.of(deletedHistory));
            given(annualLeaveDispatchHistoryRepository.findAll())
                    .willReturn(List.of(deletedHistory, remainingHistory));

            MultipartFile remainingFile = createMockExcelFile();
            given(s3Service.downloadFile("annual-leave/2026-08.xlsx"))
                    .willReturn(remainingFile.getBytes());

            User targetUser =
                    User.builder().nameKor("테스트 유저").hireDate(LocalDate.of(2025, 12, 31)).build();
            given(userRepository.findByNameAndJoinDate(anyString(), any()))
                    .willReturn(Optional.of(targetUser));
            given(annualLeaveRepository.findByUser(targetUser)).willReturn(Optional.empty());

            // when
            annualLeaveService.deleteAnnualLeaveDispatchForAdmin(adminId, dispatchId);

            // then
            verify(annualLeaveDispatchHistoryRepository).delete(deletedHistory);
            verify(annualLeaveRepository).deleteAllInBatch();
            verify(s3Service).downloadFile("annual-leave/2026-08.xlsx");
            verify(annualLeaveRepository, atLeastOnce()).save(any(AnnualLeave.class));
            org.mockito.Mockito.verify(notificationService, org.mockito.Mockito.never())
                    .sendAnnualLeaveAlertToUser(any(), anyString());
            verify(s3Service).deleteFile("annual-leave/2026-09.xlsx");
        }
    }

    @Nested
    @DisplayName("getAnnualLeaveDispatchDetailForAdmin 메서드는")
    class Describe_getAnnualLeaveDispatchDetailForAdmin {

        @Test
        @DisplayName("연차 관리 권한이 없는 유저가 요청하면 NO_AUTHORITY_FOR_ANNUAL_LEAVE 예외를 던진다")
        void it_throws_when_user_has_no_authority() {
            // given
            Long userId = 1L;
            given(userRepository.findById(userId)).willReturn(Optional.of(createMockUser(null)));

            // when & then
            assertThatThrownBy(
                            () ->
                                    annualLeaveService.getAnnualLeaveDispatchDetailForAdmin(
                                            userId, 10L))
                    .isInstanceOf(CustomException.class)
                    .hasMessageContaining("연차 관리 권한이 없습니다.");
        }

        @Test
        @DisplayName("해당 발송 이력이 없으면 ANNUAL_LEAVE_DISPATCH_HISTORY_NOT_FOUND 예외를 던진다")
        void it_throws_when_history_not_found() {
            // given
            Long adminId = 1L;
            User admin = createMockUser(Authority.MANAGE_ANNUAL_LEAVE);
            given(userRepository.findById(adminId)).willReturn(Optional.of(admin));
            given(annualLeaveDispatchHistoryRepository.findById(999L)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(
                            () ->
                                    annualLeaveService.getAnnualLeaveDispatchDetailForAdmin(
                                            adminId, 999L))
                    .isInstanceOf(CustomException.class)
                    .hasMessageContaining("해당 연차 발송 이력을 찾을 수 없습니다.");
        }

        @Test
        @DisplayName("권한이 있는 관리자가 요청하면 발송 상세와 파일 URL을 반환한다")
        void it_returns_dispatch_detail() {
            // given
            Long adminId = 1L;
            User admin = createMockUser(Authority.MANAGE_ANNUAL_LEAVE);
            admin.setPosition(Position.MANAGER);
            given(userRepository.findById(adminId)).willReturn(Optional.of(admin));

            AnnualLeaveDispatchHistory history =
                    AnnualLeaveDispatchHistory.builder()
                            .id(10L)
                            .uploadedBy(admin)
                            .originalFileName("2026_연차현황.xlsx")
                            .sheetName("2026-06")
                            .fileKey("annual-leave/2026-06-first.xlsx")
                            .createdAt(LocalDateTime.of(2026, 5, 31, 15, 29, 4))
                            .company(Company.AWESOME)
                            .build();

            given(annualLeaveDispatchHistoryRepository.findById(10L))
                    .willReturn(Optional.of(history));
            given(
                            s3Service.getPresignedDownloadUrl(
                                    "annual-leave/2026-06-first.xlsx", "2026_연차현황.xlsx"))
                    .willReturn("https://example.com/annual-leave-2026-06-first");

            // when
            AdminAnnualLeaveDispatchDetailResponseDto result =
                    annualLeaveService.getAnnualLeaveDispatchDetailForAdmin(adminId, 10L);

            // then
            assertThat(result.getDispatchId()).isEqualTo(10L);
            assertThat(result.getOriginalFileName()).isEqualTo("2026_연차현황.xlsx");
            assertThat(result.getSheetName()).isEqualTo("2026-06");
            assertThat(result.getSenderName()).isEqualTo("테스트 업로더");
            assertThat(result.getSenderPosition()).isEqualTo("과장");
            assertThat(result.getFileUrl())
                    .isEqualTo("https://example.com/annual-leave-2026-06-first");
            assertThat(result.getTitle()).isEqualTo("6월");
            assertThat(result.getCompany()).isEqualTo(Company.AWESOME);
        }
    }

    @Nested
    @DisplayName("rebuildAnnualLeavesForAdmin 메서드는")
    class Describe_rebuildAnnualLeavesForAdmin {

        @Test
        @DisplayName("연차 관리 권한이 없는 유저가 요청하면 NO_AUTHORITY_FOR_ANNUAL_LEAVE 예외를 던진다")
        void it_throws_when_user_has_no_authority() {
            // given
            Long userId = 1L;
            given(userRepository.findById(userId)).willReturn(Optional.of(createMockUser(null)));

            // when & then
            assertThatThrownBy(() -> annualLeaveService.rebuildAnnualLeavesForAdmin(userId))
                    .isInstanceOf(CustomException.class)
                    .hasMessageContaining("연차 관리 권한이 없습니다.");
        }

        @Test
        @DisplayName("권한이 있는 관리자가 요청하면 남은 발송 이력 기준으로 연차 정보를 다시 계산한다")
        void it_rebuilds_annual_leaves() throws IOException {
            // given
            Long adminId = 1L;
            User admin = createMockUser(Authority.MANAGE_ANNUAL_LEAVE);
            given(userRepository.findById(adminId)).willReturn(Optional.of(admin));

            AnnualLeaveDispatchHistory history =
                    AnnualLeaveDispatchHistory.builder()
                            .id(9L)
                            .uploadedBy(admin)
                            .originalFileName("2026_연차현황_8월.xlsx")
                            .sheetName("8월")
                            .fileKey("annual-leave/2026-08.xlsx")
                            .baseDate(LocalDate.of(2026, 8, 1))
                            .build();
            given(annualLeaveDispatchHistoryRepository.findAll()).willReturn(List.of(history));

            MultipartFile remainingFile = createMockExcelFile();
            given(s3Service.downloadFile("annual-leave/2026-08.xlsx"))
                    .willReturn(remainingFile.getBytes());

            User targetUser =
                    User.builder().nameKor("테스트 유저").hireDate(LocalDate.of(2025, 12, 31)).build();
            given(userRepository.findByNameAndJoinDate(anyString(), any()))
                    .willReturn(Optional.of(targetUser));
            given(annualLeaveRepository.findByUser(targetUser)).willReturn(Optional.empty());

            // when
            annualLeaveService.rebuildAnnualLeavesForAdmin(adminId);

            // then
            verify(annualLeaveRepository).deleteAllInBatch();
            verify(s3Service).downloadFile("annual-leave/2026-08.xlsx");
            verify(annualLeaveRepository, atLeastOnce()).save(any(AnnualLeave.class));
            org.mockito.Mockito.verify(notificationService, org.mockito.Mockito.never())
                    .sendAnnualLeaveAlertToUser(any(), anyString());
        }
    }

    @Nested
    @DisplayName("AnnualLeaveDispatchHistory 엔티티는")
    class Describe_AnnualLeaveDispatchHistory {

        @Nested
        @DisplayName("baseDate 필드를 포함하여 빌더로 생성하면")
        class Context_with_baseDate {

            @Test
            @DisplayName("baseDate 값이 정상적으로 저장된다")
            void it_stores_baseDate() {
                // given
                LocalDate expectedBaseDate = LocalDate.of(2026, 6, 1);
                User admin = User.builder().id(1L).nameKor("테스트 업로더").build();

                // when
                AnnualLeaveDispatchHistory history =
                        AnnualLeaveDispatchHistory.builder()
                                .id(10L)
                                .uploadedBy(admin)
                                .originalFileName("2026_연차현황.xlsx")
                                .sheetName("2026-06")
                                .fileKey("annual-leave/2026-06.xlsx")
                                .baseDate(expectedBaseDate)
                                .build();

                // then
                assertThat(history.getBaseDate()).isEqualTo(expectedBaseDate);
            }

            @Test
            @DisplayName("baseDate 없이 빌더로 생성하면 null을 허용한다 (nullable = true)")
            void it_allows_null_baseDate() {
                // given
                User admin = User.builder().id(1L).nameKor("테스트 업로더").build();

                // when
                AnnualLeaveDispatchHistory history =
                        AnnualLeaveDispatchHistory.builder()
                                .id(11L)
                                .uploadedBy(admin)
                                .originalFileName("legacy_2025.xlsx")
                                .sheetName("2025-12")
                                .fileKey("annual-leave/legacy.xlsx")
                                .build();

                // then
                assertThat(history.getBaseDate()).isNull();
            }
        }

        @Nested
        @DisplayName("company 필드는")
        class Context_with_company {

            @Test
            @DisplayName("company 값을 포함하여 빌더로 생성하면 getCompany()가 해당 값을 반환한다")
            void it_stores_company() {
                // given
                User admin = User.builder().id(1L).nameKor("테스트 업로더").build();

                // when
                AnnualLeaveDispatchHistory history =
                        AnnualLeaveDispatchHistory.builder()
                                .id(20L)
                                .uploadedBy(admin)
                                .originalFileName("2026_연차현황.xlsx")
                                .sheetName("2026-06")
                                .fileKey("annual-leave/2026-06.xlsx")
                                .baseDate(LocalDate.of(2026, 6, 1))
                                .company(Company.AWESOME)
                                .build();

                // then
                assertThat(history.getCompany()).isEqualTo(Company.AWESOME);
            }

            @Test
            @DisplayName("company 없이 빌더로 생성하면 null을 반환한다")
            void it_returns_null_when_company_not_set() {
                // given
                User admin = User.builder().id(1L).nameKor("테스트 업로더").build();

                // when
                AnnualLeaveDispatchHistory history =
                        AnnualLeaveDispatchHistory.builder()
                                .id(21L)
                                .uploadedBy(admin)
                                .originalFileName("legacy_2025.xlsx")
                                .sheetName("2025-12")
                                .fileKey("annual-leave/legacy.xlsx")
                                .build();

                // then
                assertThat(history.getCompany()).isNull();
            }
        }
    }

    @Nested
    @DisplayName("ErrorCode enum은")
    class Describe_ErrorCode {

        @Nested
        @DisplayName("DUPLICATE_ANNUAL_LEAVE_DISPATCH 상수가")
        class Context_DUPLICATE_ANNUAL_LEAVE_DISPATCH {

            @Test
            @DisplayName("정의되어 있어야 하고 HttpStatus.CONFLICT와 지정된 메시지를 가진다")
            void it_has_correct_status_and_message() {
                // given & when
                kr.co.awesomelead.groupware_backend.global.error.ErrorCode code =
                        kr.co.awesomelead.groupware_backend.global.error.ErrorCode.valueOf(
                                "DUPLICATE_ANNUAL_LEAVE_DISPATCH");

                // then
                assertThat(code.getHttpStatus())
                        .isEqualTo(org.springframework.http.HttpStatus.CONFLICT);
                assertThat(code.getMessage()).isEqualTo("이미 해당 월에 연차가 발송되었습니다.");
            }
        }

        @Nested
        @DisplayName("ANNUAL_LEAVE_SHEET_NOT_FOUND 상수가")
        class Context_ANNUAL_LEAVE_SHEET_NOT_FOUND {

            @Test
            @DisplayName("정의되어 있어야 하고 HttpStatus.BAD_REQUEST와 지정된 메시지를 가진다")
            void it_has_correct_status_and_message() {
                // given & when
                kr.co.awesomelead.groupware_backend.global.error.ErrorCode code =
                        kr.co.awesomelead.groupware_backend.global.error.ErrorCode.valueOf(
                                "ANNUAL_LEAVE_SHEET_NOT_FOUND");

                // then
                assertThat(code.getHttpStatus())
                        .isEqualTo(org.springframework.http.HttpStatus.BAD_REQUEST);
                assertThat(code.getMessage()).isEqualTo("입력한 시트명을 엑셀 파일에서 찾을 수 없습니다.");
            }
        }

        @Nested
        @DisplayName("ANNUAL_LEAVE_INVALID_SHEET_NAME 상수가")
        class Context_ANNUAL_LEAVE_INVALID_SHEET_NAME {

            @Test
            @DisplayName("정의되어 있어야 하고 HttpStatus.BAD_REQUEST와 지정된 메시지를 가진다")
            void it_has_correct_status_and_message() {
                // given & when
                kr.co.awesomelead.groupware_backend.global.error.ErrorCode code =
                        kr.co.awesomelead.groupware_backend.global.error.ErrorCode.valueOf(
                                "ANNUAL_LEAVE_INVALID_SHEET_NAME");

                // then
                assertThat(code.getHttpStatus())
                        .isEqualTo(org.springframework.http.HttpStatus.BAD_REQUEST);
                assertThat(code.getMessage()).isEqualTo("시트명에는 월 외의 날짜 숫자를 포함할 수 없습니다.");
            }
        }

        @Nested
        @DisplayName("ANNUAL_LEAVE_DISPATCH_MONTH_MISMATCH 상수가")
        class Context_ANNUAL_LEAVE_DISPATCH_MONTH_MISMATCH {

            @Test
            @DisplayName("정의되어 있어야 하고 HttpStatus.BAD_REQUEST와 지정된 메시지를 가진다")
            void it_has_correct_status_and_message() {
                // given & when
                kr.co.awesomelead.groupware_backend.global.error.ErrorCode code =
                        kr.co.awesomelead.groupware_backend.global.error.ErrorCode.valueOf(
                                "ANNUAL_LEAVE_DISPATCH_MONTH_MISMATCH");

                // then
                assertThat(code.getHttpStatus())
                        .isEqualTo(org.springframework.http.HttpStatus.BAD_REQUEST);
                assertThat(code.getMessage()).isEqualTo("수정하려는 연차 발송 월과 엑셀 기준일의 월이 일치하지 않습니다.");
            }
        }

        @Nested
        @DisplayName("ANNUAL_LEAVE_MONTH_MISMATCH 상수가")
        class Context_ANNUAL_LEAVE_MONTH_MISMATCH {

            @Test
            @DisplayName("정의되어 있어야 하고 HttpStatus.BAD_REQUEST와 지정된 메시지를 가진다")
            void it_has_correct_status_and_message() {
                // given & when
                kr.co.awesomelead.groupware_backend.global.error.ErrorCode code =
                        kr.co.awesomelead.groupware_backend.global.error.ErrorCode.valueOf(
                                "ANNUAL_LEAVE_MONTH_MISMATCH");

                // then
                assertThat(code.getHttpStatus())
                        .isEqualTo(org.springframework.http.HttpStatus.BAD_REQUEST);
                assertThat(code.getMessage()).isEqualTo("시트명의 월 정보와 엑셀 기준일의 월 정보가 일치하지 않습니다.");
            }
        }

        @Nested
        @DisplayName("ANNUAL_LEAVE_COMPANY_MISMATCH 상수가")
        class Context_ANNUAL_LEAVE_COMPANY_MISMATCH {

            @Test
            @DisplayName("정의되어 있어야 하고 HttpStatus.BAD_REQUEST와 지정된 메시지를 가진다")
            void it_has_correct_status_and_message() {
                // given & when
                kr.co.awesomelead.groupware_backend.global.error.ErrorCode code =
                        kr.co.awesomelead.groupware_backend.global.error.ErrorCode.valueOf(
                                "ANNUAL_LEAVE_COMPANY_MISMATCH");

                // then
                assertThat(code.getHttpStatus())
                        .isEqualTo(org.springframework.http.HttpStatus.BAD_REQUEST);
                assertThat(code.getMessage())
                        .isEqualTo("업로드 요청한 회사 정보와 엑셀 데이터 내 직원의 소속 회사가 일치하지 않습니다.");
            }
        }
    }

    @Nested
    @DisplayName("uploadAnnualLeaveFile 중복 발송 체크는")
    class Describe_uploadAnnualLeaveFile_duplicateCheck {

        private final Long loginUserId = 1L;
        private final String sheetName = "8월";

        @Nested
        @DisplayName("같은 월에 이미 발송 이력이 존재하면")
        class Context_when_duplicate_exists {

            @Test
            @DisplayName("DUPLICATE_ANNUAL_LEAVE_DISPATCH 예외를 던진다")
            void uploadAnnualLeaveFile_throwsWhenDuplicateMonth() throws IOException {
                // given
                User admin = createMockUser(Authority.MANAGE_ANNUAL_LEAVE);
                given(userRepository.findById(loginUserId)).willReturn(Optional.of(admin));

                // 엑셀 파일의 baseDate는 2026-08-01이므로 중복 체크는 8월 기준으로 수행
                LocalDate start = LocalDate.of(2026, 8, 1);
                LocalDate end = LocalDate.of(2026, 8, 31);
                given(
                                annualLeaveDispatchHistoryRepository
                                        .existsByCompanyAndBaseDateBetween(
                                                Company.AWESOME, start, end))
                        .willReturn(true);

                MultipartFile mockFile = createMockExcelFile();

                // when & then
                assertThatThrownBy(
                                () ->
                                        annualLeaveService.uploadAnnualLeaveFile(
                                                mockFile, sheetName, loginUserId, Company.AWESOME))
                        .isInstanceOf(CustomException.class)
                        .hasMessageContaining("이미 해당 월에 연차가 발송되었습니다.");
            }
        }

        @Nested
        @DisplayName("같은 월에 발송 이력이 없으면")
        class Context_when_no_duplicate {

            @Test
            @DisplayName("중복 체크를 통과하고 발송이 정상 처리된다")
            void uploadAnnualLeaveFile_succeedsWhenNoDuplicate() throws IOException {
                // given
                User admin = createMockUser(Authority.MANAGE_ANNUAL_LEAVE);
                given(userRepository.findById(loginUserId)).willReturn(Optional.of(admin));

                // 엑셀 파일의 baseDate는 2026-08-01이므로 중복 체크는 8월 기준으로 수행
                LocalDate start = LocalDate.of(2026, 8, 1);
                LocalDate end = LocalDate.of(2026, 8, 31);
                given(
                                annualLeaveDispatchHistoryRepository
                                        .existsByCompanyAndBaseDateBetween(
                                                Company.AWESOME, start, end))
                        .willReturn(false);

                MultipartFile mockFile = createMockExcelFile();
                User targetUser =
                        User.builder()
                                .nameKor("테스트 유저")
                                .hireDate(LocalDate.of(2025, 12, 31))
                                .build();
                given(userRepository.findByNameAndJoinDate(anyString(), any()))
                        .willReturn(Optional.of(targetUser));
                given(annualLeaveRepository.findByUser(targetUser)).willReturn(Optional.empty());
                given(s3Service.uploadFile(mockFile)).willReturn("annual-leave/2026-08.xlsx");

                // when
                ExcelUploadResponseDto response =
                        annualLeaveService.uploadAnnualLeaveFile(
                                mockFile, sheetName, loginUserId, Company.AWESOME);

                // then
                assertThat(response.getSuccessCount()).isGreaterThan(0);
            }
        }
    }

    @Nested
    @DisplayName("uploadAnnualLeaveFile 시트명-기준일 교차 검증은")
    class Describe_uploadAnnualLeaveFile_monthMismatch {

        private final Long loginUserId = 1L;

        @Nested
        @DisplayName("시트명의 월과 엑셀 기준일의 월이 불일치하면")
        class Context_with_month_mismatch {

            @Test
            @DisplayName("ANNUAL_LEAVE_MONTH_MISMATCH 예외를 던진다")
            void uploadAnnualLeaveFile_throwsWhenMonthMismatch() throws IOException {
                // given: 엑셀 파일의 기준일은 "2026-08-01"(8월), 시트명은 "5월"로 불일치
                User admin = createMockUser(Authority.MANAGE_ANNUAL_LEAVE);
                given(userRepository.findById(loginUserId)).willReturn(Optional.of(admin));

                MultipartFile mockFile = createMockExcelFileWithSheetName("5월");

                // when & then
                assertThatThrownBy(
                                () ->
                                        annualLeaveService.uploadAnnualLeaveFile(
                                                mockFile, "5월", loginUserId, Company.AWESOME))
                        .isInstanceOf(CustomException.class)
                        .hasMessageContaining("시트명의 월 정보와 엑셀 기준일의 월 정보가 일치하지 않습니다.");
            }
        }

        @Nested
        @DisplayName("시트명이 yyyy-MM 형식이고 기준일 월과 불일치하면")
        class Context_with_year_month_sheet_name_mismatch {

            @Test
            @DisplayName("ANNUAL_LEAVE_MONTH_MISMATCH 예외를 던진다")
            void uploadAnnualLeaveFile_throwsWhenYearMonthSheetNameMismatch() throws IOException {
                // given
                User admin = createMockUser(Authority.MANAGE_ANNUAL_LEAVE);
                given(userRepository.findById(loginUserId)).willReturn(Optional.of(admin));

                MultipartFile mockFile = createMockExcelFileWithSheetName("2026-06");

                // when & then
                assertThatThrownBy(
                                () ->
                                        annualLeaveService.uploadAnnualLeaveFile(
                                                mockFile, "2026-06", loginUserId, Company.AWESOME))
                        .isInstanceOf(CustomException.class)
                        .hasMessageContaining("시트명의 월 정보와 엑셀 기준일의 월 정보가 일치하지 않습니다.");
            }
        }

        @Nested
        @DisplayName("시트명에 월 외의 일자 숫자가 포함되면")
        class Context_with_day_in_sheet_name {

            @Test
            @DisplayName("ANNUAL_LEAVE_INVALID_SHEET_NAME 예외를 던진다")
            void uploadAnnualLeaveFile_throwsWhenSheetNameContainsDay() throws IOException {
                // given
                User admin = createMockUser(Authority.MANAGE_ANNUAL_LEAVE);
                given(userRepository.findById(loginUserId)).willReturn(Optional.of(admin));

                MultipartFile mockFile = createMockExcelFileWithSheetName("8월 1일 연차현황");

                // when & then
                assertThatThrownBy(
                                () ->
                                        annualLeaveService.uploadAnnualLeaveFile(
                                                mockFile,
                                                "8월 1일 연차현황",
                                                loginUserId,
                                                Company.AWESOME))
                        .isInstanceOf(CustomException.class)
                        .hasMessageContaining("시트명에는 월 외의 날짜 숫자를 포함할 수 없습니다.");
            }
        }
    }

    @Nested
    @DisplayName("processAnnualLeaveRow 기존 연차 기준월 비교는")
    class Describe_processAnnualLeaveRow_skipWhenNewerExists {

        private final Long loginUserId = 1L;
        private final String sheetName = "8월";

        @Nested
        @DisplayName("기존 직원의 연차 updateDate YearMonth가 업로드하는 updateDate YearMonth보다 최신이면")
        class Context_with_past_month_data {

            @Test
            @DisplayName("annualLeaveRepository.save 및 알림 전송을 호출하지 않는다")
            void processAnnualLeaveRow_skipsWhenExistingDateIsNewer() throws IOException {
                // given: 엑셀 파일 기준일은 2026-08-01, 기존 직원의 연차 updateDate는 2026-09-01 (더 최신)
                User admin = createMockUser(Authority.MANAGE_ANNUAL_LEAVE);
                given(userRepository.findById(loginUserId)).willReturn(Optional.of(admin));

                MultipartFile mockFile = createMockExcelFile();

                User targetUser =
                        User.builder()
                                .nameKor("테스트 유저")
                                .hireDate(LocalDate.of(2025, 12, 31))
                                .build();
                given(userRepository.findByNameAndJoinDate(anyString(), any()))
                        .willReturn(Optional.of(targetUser));

                // 기존 연차의 updateDate가 엑셀 기준일(2026-08)보다 1개월 더 최신인 2026-09
                AnnualLeave existingNewerAnnualLeave =
                        AnnualLeave.builder()
                                .id(1L)
                                .user(targetUser)
                                .total(15.0)
                                .used(3.0)
                                .remain(12.0)
                                .updateDate(LocalDate.of(2026, 9, 1))
                                .build();
                given(annualLeaveRepository.findByUser(targetUser))
                        .willReturn(Optional.of(existingNewerAnnualLeave));
                given(s3Service.uploadFile(mockFile)).willReturn("annual-leave/2026-08.xlsx");

                // when
                ExcelUploadResponseDto response =
                        annualLeaveService.uploadAnnualLeaveFile(
                                mockFile, sheetName, loginUserId, Company.AWESOME);

                // then: 기존 연차가 더 최신이므로 save와 알림 전송이 호출되지 않아야 한다
                org.mockito.Mockito.verify(annualLeaveRepository, org.mockito.Mockito.never())
                        .save(existingNewerAnnualLeave);
                org.mockito.Mockito.verify(notificationService, org.mockito.Mockito.never())
                        .sendAnnualLeaveAlertToUser(
                                org.mockito.ArgumentMatchers.eq(targetUser.getId()), anyString());
            }
        }
    }

    @Nested
    @DisplayName("updateAnnualLeaveDispatchForAdmin 중복 발송 체크는")
    class Describe_updateAnnualLeaveDispatchForAdmin_duplicateCheck {

        private final Long adminId = 1L;
        private final Long dispatchId = 10L;
        private final String sheetName = "8월";

        @Nested
        @DisplayName("수정 대상 월과 엑셀 기준월이 다르면")
        class Context_when_dispatch_month_mismatch {

            @Test
            @DisplayName("ANNUAL_LEAVE_DISPATCH_MONTH_MISMATCH 예외를 던진다")
            void updateAnnualLeaveDispatchForAdmin_throwsWhenDispatchMonthMismatch()
                    throws IOException {
                // given
                User admin = createMockUser(Authority.MANAGE_ANNUAL_LEAVE);
                given(userRepository.findById(adminId)).willReturn(Optional.of(admin));

                AnnualLeaveDispatchHistory history =
                        AnnualLeaveDispatchHistory.builder()
                                .id(dispatchId)
                                .uploadedBy(admin)
                                .originalFileName("old.xlsx")
                                .sheetName("6월")
                                .fileKey("annual-leave/old.xlsx")
                                .baseDate(LocalDate.of(2026, 6, 1))
                                .company(Company.AWESOME)
                                .build();
                given(annualLeaveDispatchHistoryRepository.findById(dispatchId))
                        .willReturn(Optional.of(history));

                MultipartFile mockFile = createMockExcelFile();

                // when & then
                assertThatThrownBy(
                                () ->
                                        annualLeaveService.updateAnnualLeaveDispatchForAdmin(
                                                adminId,
                                                dispatchId,
                                                mockFile,
                                                sheetName,
                                                Company.AWESOME))
                        .isInstanceOf(CustomException.class)
                        .hasMessageContaining("수정하려는 연차 발송 월과 엑셀 기준일의 월이 일치하지 않습니다.");
                org.mockito.Mockito.verify(s3Service, org.mockito.Mockito.never())
                        .uploadFile(mockFile);
            }
        }

        @Nested
        @DisplayName("자기 자신을 제외한 같은 월에 이미 발송 이력이 존재하면")
        class Context_when_duplicate_exists_excluding_self {

            @Test
            @DisplayName("DUPLICATE_ANNUAL_LEAVE_DISPATCH 예외를 던진다")
            void updateAnnualLeaveDispatchForAdmin_throwsWhenDuplicateExcludingSelf()
                    throws IOException {
                // given
                User admin = createMockUser(Authority.MANAGE_ANNUAL_LEAVE);
                given(userRepository.findById(adminId)).willReturn(Optional.of(admin));

                AnnualLeaveDispatchHistory history =
                        AnnualLeaveDispatchHistory.builder()
                                .id(dispatchId)
                                .uploadedBy(admin)
                                .originalFileName("old.xlsx")
                                .sheetName("8월")
                                .fileKey("annual-leave/old.xlsx")
                                .baseDate(LocalDate.of(2026, 8, 1))
                                .company(Company.AWESOME)
                                .build();
                given(annualLeaveDispatchHistoryRepository.findById(dispatchId))
                        .willReturn(Optional.of(history));

                // 엑셀 파일의 baseDate는 2026-08-01이므로 중복 체크는 8월 기준으로 수행
                LocalDate start = LocalDate.of(2026, 8, 1);
                LocalDate end = LocalDate.of(2026, 8, 31);
                given(
                                annualLeaveDispatchHistoryRepository
                                        .existsByIdNotAndCompanyAndBaseDateBetween(
                                                dispatchId, Company.AWESOME, start, end))
                        .willReturn(true);

                MultipartFile mockFile = createMockExcelFile();

                // when & then
                assertThatThrownBy(
                                () ->
                                        annualLeaveService.updateAnnualLeaveDispatchForAdmin(
                                                adminId,
                                                dispatchId,
                                                mockFile,
                                                sheetName,
                                                Company.AWESOME))
                        .isInstanceOf(CustomException.class)
                        .hasMessageContaining("이미 해당 월에 연차가 발송되었습니다.");
            }
        }

        @Nested
        @DisplayName("자기 자신을 제외한 같은 월에 발송 이력이 없으면")
        class Context_when_no_duplicate_excluding_self {

            @Test
            @DisplayName("중복 체크를 통과하고 수정이 정상 처리된다")
            void updateAnnualLeaveDispatchForAdmin_succeedsWhenNoDuplicateExcludingSelf()
                    throws IOException {
                // given
                User admin = createMockUser(Authority.MANAGE_ANNUAL_LEAVE);
                given(userRepository.findById(adminId)).willReturn(Optional.of(admin));

                AnnualLeaveDispatchHistory history =
                        AnnualLeaveDispatchHistory.builder()
                                .id(dispatchId)
                                .uploadedBy(admin)
                                .originalFileName("old.xlsx")
                                .sheetName("8월")
                                .fileKey("annual-leave/old.xlsx")
                                .baseDate(LocalDate.of(2026, 8, 1))
                                .company(Company.AWESOME)
                                .build();
                given(annualLeaveDispatchHistoryRepository.findById(dispatchId))
                        .willReturn(Optional.of(history));

                // 엑셀 파일의 baseDate는 2026-08-01이므로 중복 체크는 8월 기준으로 수행
                LocalDate start = LocalDate.of(2026, 8, 1);
                LocalDate end = LocalDate.of(2026, 8, 31);
                given(
                                annualLeaveDispatchHistoryRepository
                                        .existsByIdNotAndCompanyAndBaseDateBetween(
                                                dispatchId, Company.AWESOME, start, end))
                        .willReturn(false);

                MultipartFile mockFile = createMockExcelFile();
                User targetUser =
                        User.builder()
                                .nameKor("테스트 유저")
                                .hireDate(LocalDate.of(2025, 12, 31))
                                .build();
                given(userRepository.findByNameAndJoinDate(anyString(), any()))
                        .willReturn(Optional.of(targetUser));
                given(annualLeaveRepository.findByUser(targetUser)).willReturn(Optional.empty());
                given(s3Service.uploadFile(mockFile)).willReturn("annual-leave/2026-08.xlsx");

                // when
                ExcelUploadResponseDto result =
                        annualLeaveService.updateAnnualLeaveDispatchForAdmin(
                                adminId, dispatchId, mockFile, sheetName, Company.AWESOME);

                // then
                assertThat(result.getSuccessCount()).isGreaterThan(0);
            }
        }
    }

    @Nested
    @DisplayName("AnnualLeaveDispatchHistoryRepository는")
    class Describe_AnnualLeaveDispatchHistoryRepository {

        @Nested
        @DisplayName("company 기반 쿼리 메서드가")
        class Context_company_based_methods {

            @Test
            @DisplayName("findAllByCompanyOrderByCreatedAtDesc 메서드가 컴파일 레벨에서 호출 가능하다")
            void findAllByCompanyOrderByCreatedAtDesc_methodExists() {
                // given & when
                java.util.List<AnnualLeaveDispatchHistory> result =
                        annualLeaveDispatchHistoryRepository.findAllByCompanyOrderByCreatedAtDesc(
                                Company.AWESOME);

                // then: mock 기본값은 빈 리스트 — 메서드 존재 자체를 컴파일 레벨에서 검증
                assertThat(result).isNotNull();
            }

            @Test
            @DisplayName("existsByCompanyAndBaseDateBetween 메서드가 컴파일 레벨에서 호출 가능하다")
            void existsByCompanyAndBaseDateBetween_methodExists() {
                // given
                LocalDate start = LocalDate.of(2026, 6, 1);
                LocalDate end = LocalDate.of(2026, 6, 30);

                // when
                boolean result =
                        annualLeaveDispatchHistoryRepository.existsByCompanyAndBaseDateBetween(
                                Company.AWESOME, start, end);

                // then: mock 기본값은 false — 메서드 존재 자체를 컴파일 레벨에서 검증
                assertThat(result).isFalse();
            }

            @Test
            @DisplayName("existsByIdNotAndCompanyAndBaseDateBetween 메서드가 컴파일 레벨에서 호출 가능하다")
            void existsByIdNotAndCompanyAndBaseDateBetween_methodExists() {
                // given
                LocalDate start = LocalDate.of(2026, 6, 1);
                LocalDate end = LocalDate.of(2026, 6, 30);

                // when
                boolean result =
                        annualLeaveDispatchHistoryRepository
                                .existsByIdNotAndCompanyAndBaseDateBetween(
                                        10L, Company.AWESOME, start, end);

                // then: mock 기본값은 false — 메서드 존재 자체를 컴파일 레벨에서 검증
                assertThat(result).isFalse();
            }
        }
    }

    @Nested
    @DisplayName("AdminAnnualLeaveDispatchDetailResponseDto는")
    class Describe_AdminAnnualLeaveDispatchDetailResponseDto {

        @Nested
        @DisplayName("title과 company 필드를 포함하여 빌더로 생성하면")
        class Context_with_title_and_company {

            @Test
            @DisplayName("각각 올바른 값을 반환한다")
            void it_returns_title_and_company() {
                // given
                String expectedTitle = "7월";
                Company expectedCompany = Company.AWESOME;

                // when
                AdminAnnualLeaveDispatchDetailResponseDto dto =
                        AdminAnnualLeaveDispatchDetailResponseDto.builder()
                                .dispatchId(10L)
                                .originalFileName("2026_연차현황.xlsx")
                                .sheetName("2026-07")
                                .senderName("김관리")
                                .senderPosition("과장")
                                .title(expectedTitle)
                                .company(expectedCompany)
                                .build();

                // then
                assertThat(dto.getSenderName()).isEqualTo("김관리");
                assertThat(dto.getSenderPosition()).isEqualTo("과장");
                assertThat(dto.getTitle()).isEqualTo(expectedTitle);
                assertThat(dto.getCompany()).isEqualTo(expectedCompany);
            }
        }
    }

    @Nested
    @DisplayName("AdminAnnualLeaveDispatchItemResponseDto는")
    class Describe_AdminAnnualLeaveDispatchItemResponseDto {

        @Nested
        @DisplayName("company 필드를 포함하여 빌더로 생성하면")
        class Context_with_company {

            @Test
            @DisplayName("올바른 값을 반환한다")
            void it_returns_company() {
                // given
                Company expectedCompany = Company.AWESOME;

                // when
                AdminAnnualLeaveDispatchItemResponseDto dto =
                        AdminAnnualLeaveDispatchItemResponseDto.builder()
                                .dispatchId(10L)
                                .title("7월")
                                .originalFileName("2026_연차현황.xlsx")
                                .sheetName("2026-07")
                                .company(expectedCompany)
                                .build();

                // then
                assertThat(dto.getCompany()).isEqualTo(expectedCompany);
            }
        }
    }

    @Nested
    @DisplayName("uploadAnnualLeaveFile Company별 중복 발송 체크는")
    class Describe_uploadAnnualLeaveFile_company_duplicate_check {

        private final Long loginUserId = 1L;
        private final String sheetName = "8월";

        @Nested
        @DisplayName("같은 Company + 같은 월에 이미 이력이 존재하면")
        class Context_when_same_company_same_month_exists {

            @Test
            @DisplayName("DUPLICATE_ANNUAL_LEAVE_DISPATCH 예외를 던진다")
            void uploadAnnualLeaveFile_throwsWhenSameCompanySameMonthExists() throws IOException {
                // given
                User admin = createMockUser(Authority.MANAGE_ANNUAL_LEAVE);
                given(userRepository.findById(loginUserId)).willReturn(Optional.of(admin));

                // 엑셀 파일의 baseDate는 2026-08-01이므로 중복 체크는 8월 기준으로 수행
                LocalDate start = LocalDate.of(2026, 8, 1);
                LocalDate end = LocalDate.of(2026, 8, 31);
                given(
                                annualLeaveDispatchHistoryRepository
                                        .existsByCompanyAndBaseDateBetween(
                                                Company.MARUI, start, end))
                        .willReturn(true);

                MultipartFile mockFile = createMockExcelFile();

                // when & then
                assertThatThrownBy(
                                () ->
                                        annualLeaveService.uploadAnnualLeaveFile(
                                                mockFile, sheetName, loginUserId, Company.MARUI))
                        .isInstanceOf(CustomException.class)
                        .hasMessageContaining("이미 해당 월에 연차가 발송되었습니다.");
            }
        }

        @Nested
        @DisplayName("다른 Company + 같은 월에 이력이 존재해도")
        class Context_when_different_company_same_month {

            @Test
            @DisplayName("중복으로 보지 않고 정상 처리된다")
            void uploadAnnualLeaveFile_succeedsWhenDifferentCompanySameMonth() throws IOException {
                // given: MARUI 6월 이력 있음 → AWESOME 8월은 중복 아님 (엑셀 baseDate 기준)
                User admin = createMockUser(Authority.MANAGE_ANNUAL_LEAVE);
                given(userRepository.findById(loginUserId)).willReturn(Optional.of(admin));

                // 엑셀 파일의 baseDate는 2026-08-01이므로 중복 체크는 8월 기준으로 수행
                LocalDate start = LocalDate.of(2026, 8, 1);
                LocalDate end = LocalDate.of(2026, 8, 31);
                given(
                                annualLeaveDispatchHistoryRepository
                                        .existsByCompanyAndBaseDateBetween(
                                                Company.AWESOME, start, end))
                        .willReturn(false);

                MultipartFile mockFile = createMockExcelFile();
                User targetUser =
                        User.builder()
                                .nameKor("테스트 유저")
                                .hireDate(LocalDate.of(2025, 12, 31))
                                .workLocation(Company.AWESOME)
                                .build();
                given(userRepository.findByNameAndJoinDate(anyString(), any()))
                        .willReturn(Optional.of(targetUser));
                given(annualLeaveRepository.findByUser(targetUser)).willReturn(Optional.empty());
                given(s3Service.uploadFile(mockFile)).willReturn("annual-leave/2026-08.xlsx");

                // when
                ExcelUploadResponseDto response =
                        annualLeaveService.uploadAnnualLeaveFile(
                                mockFile, sheetName, loginUserId, Company.AWESOME);

                // then
                assertThat(response.getSuccessCount()).isGreaterThan(0);
            }
        }
    }

    @Nested
    @DisplayName("uploadAnnualLeaveFile Company 불일치 검증은")
    class Describe_uploadAnnualLeaveFile_company_mismatch {

        private final Long loginUserId = 1L;
        private final String sheetName = "8월";

        @Nested
        @DisplayName("엑셀 직원의 workLocation이 요청 Company와 다르면")
        class Context_when_user_work_location_differs_from_requested_company {

            @Test
            @DisplayName("ANNUAL_LEAVE_COMPANY_MISMATCH 예외를 던지고 전체 롤백한다")
            void uploadAnnualLeaveFile_throwsWhenCompanyMismatch() throws IOException {
                // given: 요청은 AWESOME이지만 엑셀 직원의 workLocation은 MARUI
                User admin = createMockUser(Authority.MANAGE_ANNUAL_LEAVE);
                given(userRepository.findById(loginUserId)).willReturn(Optional.of(admin));

                MultipartFile mockFile = createMockExcelFile();

                User targetUser =
                        User.builder()
                                .nameKor("테스트 유저")
                                .hireDate(LocalDate.of(2025, 12, 31))
                                .workLocation(Company.MARUI)
                                .build();
                given(userRepository.findByNameAndJoinDate(anyString(), any()))
                        .willReturn(Optional.of(targetUser));

                // when & then
                assertThatThrownBy(
                                () ->
                                        annualLeaveService.uploadAnnualLeaveFile(
                                                mockFile, sheetName, loginUserId, Company.AWESOME))
                        .isInstanceOf(CustomException.class)
                        .hasMessageContaining("업로드 요청한 회사 정보와 엑셀 데이터 내 직원의 소속 회사가 일치하지 않습니다.");
            }
        }
    }

    @Nested
    @DisplayName("getMonthlyDispatchStatus 메서드는")
    class Describe_getMonthlyDispatchStatus {

        private final Long loginUserId = 1L;

        @Nested
        @DisplayName("권한이 있는 관리자가 요청하면")
        class Context_with_authority {

            @Test
            @DisplayName("각 회사별 당월 발송 이력 존재 여부를 Map으로 반환한다")
            void getMonthlyDispatchStatus_success() {
                // given
                User admin = createMockUser(Authority.MANAGE_ANNUAL_LEAVE);
                given(userRepository.findById(loginUserId)).willReturn(Optional.of(admin));

                given(
                                annualLeaveDispatchHistoryRepository
                                        .existsByCompanyAndBaseDateBetween(
                                                org.mockito.ArgumentMatchers.eq(Company.AWESOME),
                                                any(LocalDate.class),
                                                any(LocalDate.class)))
                        .willReturn(true);
                given(
                                annualLeaveDispatchHistoryRepository
                                        .existsByCompanyAndBaseDateBetween(
                                                org.mockito.ArgumentMatchers.eq(Company.MARUI),
                                                any(LocalDate.class),
                                                any(LocalDate.class)))
                        .willReturn(false);

                // when
                java.util.Map<String, Boolean> result =
                        annualLeaveService.getMonthlyDispatchStatus(loginUserId);

                // then
                assertThat(result).isNotNull();
                assertThat(result.get("어썸리드")).isEqualTo(true);
                assertThat(result.get("한국마루이")).isEqualTo(false);

                // 키 순서가 Company.values() 순서(AWESOME → MARUI)와 일치하는지 검증
                java.util.List<String> keys = new java.util.ArrayList<>(result.keySet());
                assertThat(keys.get(0)).isEqualTo("어썸리드");
                assertThat(keys.get(1)).isEqualTo("한국마루이");
            }
        }

        @Nested
        @DisplayName("권한이 없는 사용자가 요청하면")
        class Context_without_authority {

            @Test
            @DisplayName("NO_AUTHORITY_FOR_ANNUAL_LEAVE 예외를 던진다")
            void getMonthlyDispatchStatus_throwsWhenNoAuthority() {
                // given
                User userWithoutAuth = createMockUser(null);
                given(userRepository.findById(loginUserId))
                        .willReturn(Optional.of(userWithoutAuth));

                // when & then
                assertThatThrownBy(() -> annualLeaveService.getMonthlyDispatchStatus(loginUserId))
                        .isInstanceOf(CustomException.class)
                        .extracting("errorCode")
                        .isEqualTo(ErrorCode.NO_AUTHORITY_FOR_ANNUAL_LEAVE);
            }
        }
    }

    private User createMockUser(Authority authority) { // 권한을 받아 유저를 생성하는
        User user = User.builder().id(1L).nameKor("테스트 업로더").build();
        user.addAuthority(authority); // 권한 추가
        return user;
    }

    private MultipartFile createMockExcelFile() throws IOException {
        // 1. 경로 지정 (src/test/resources 기준)
        String path = "excel/annual_leave_test.xlsx";
        ClassPathResource resource = new ClassPathResource(path);

        // 2. MockMultipartFile 생성
        // (필드명, 원본파일명, 컨텐츠타입, 바이트데이터)
        return new MockMultipartFile(
                "file",
                "annual_leave_test.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                resource.getInputStream());
    }

    private MultipartFile createMockExcelFileWithWrongDateFormat() throws IOException {
        // 1. 경로 지정 (src/test/resources 기준)
        String path = "excel/wrong_annual_leave_test.xlsx";
        ClassPathResource resource = new ClassPathResource(path);

        // 2. MockMultipartFile 생성
        // (필드명, 원본파일명, 컨텐츠타입, 바이트데이터)
        return new MockMultipartFile(
                "file",
                "wrong_annual_leave_test.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                resource.getInputStream());
    }

    private MultipartFile createMockExcelFileWithSheetName(String sheetName) throws IOException {
        ClassPathResource resource = new ClassPathResource("excel/annual_leave_test.xlsx");
        try (org.apache.poi.ss.usermodel.Workbook workbook =
                        org.apache.poi.ss.usermodel.WorkbookFactory.create(
                                resource.getInputStream());
                ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            workbook.setSheetName(0, sheetName);
            workbook.write(outputStream);
            return new MockMultipartFile(
                    "file",
                    "annual_leave_test.xlsx",
                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                    outputStream.toByteArray());
        }
    }

    private MultipartFile createMockExcelFileWithRemark(String remark) throws IOException {
        ClassPathResource resource = new ClassPathResource("excel/annual_leave_test.xlsx");
        try (org.apache.poi.ss.usermodel.Workbook workbook =
                        org.apache.poi.ss.usermodel.WorkbookFactory.create(
                                resource.getInputStream());
                ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            workbook.getSheet("8월").getRow(7).getCell(10).setCellValue(remark);
            workbook.write(outputStream);
            return new MockMultipartFile(
                    "file",
                    "annual_leave_test.xlsx",
                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                    outputStream.toByteArray());
        }
    }
}
