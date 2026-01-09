package kr.co.awesomelead.groupware_backend.domain.annualleave;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;

import java.io.IOException;
import java.time.LocalDate;
import java.util.Optional;
import kr.co.awesomelead.groupware_backend.domain.annualleave.dto.response.AnnualLeaveResponseDto;
import kr.co.awesomelead.groupware_backend.domain.annualleave.dto.response.ExcelUploadResponseDto;
import kr.co.awesomelead.groupware_backend.domain.annualleave.entity.AnnualLeave;
import kr.co.awesomelead.groupware_backend.domain.annualleave.mapper.AnnualLeaveMapper;
import kr.co.awesomelead.groupware_backend.domain.annualleave.repository.AnnualLeaveRepository;
import kr.co.awesomelead.groupware_backend.domain.annualleave.service.AnnualLeaveService;
import kr.co.awesomelead.groupware_backend.domain.user.entity.User;
import kr.co.awesomelead.groupware_backend.domain.user.enums.Authority;
import kr.co.awesomelead.groupware_backend.domain.user.repository.UserRepository;
import kr.co.awesomelead.groupware_backend.global.error.CustomException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.ClassPathResource;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

@ExtendWith(MockitoExtension.class)
public class AnnualLeaveServiceTest {

    @Mock
    private AnnualLeaveRepository annualLeaveRepository;

    @Mock
    private UserRepository userRepository;
    @Mock
    private AnnualLeaveMapper annualLeaveMapper;

    @InjectMocks
    private AnnualLeaveService annualLeaveService;

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
                            null, sheetName, loginUserId))
                    .isInstanceOf(CustomException.class)
                    .hasMessageContaining("연차 발송 권한이 없습니다.");
            }
        }

        @Nested
        @DisplayName("정상적인 엑셀 파일이 주어지면")
        class Context_with_valid_excel {

            @Test
            @DisplayName("데이터를 파싱하여 저장하고 성공 결과를 반환한다")
            void it_returns_success_response() throws IOException {
                // given
                User admin = createMockUser(Authority.MANAGE_EMPLOYEE_DATA); // 연차 발송 권한 추가
                given(userRepository.findById(loginUserId)).willReturn(Optional.of(admin));

                MultipartFile mockFile = createMockExcelFile();

                User targetUser =
                    User.builder()
                        .nameKor("테스트 유저")
                        .hireDate(LocalDate.of(2025, 12, 31))
                        .build();
                given(userRepository.findByNameAndJoinDate(anyString(), any()))
                    .willReturn(Optional.of(targetUser));
                given(annualLeaveRepository.findByUser(targetUser)).willReturn(Optional.empty());

                // when
                ExcelUploadResponseDto response =
                    annualLeaveService.uploadAnnualLeaveFile(mockFile, sheetName, loginUserId);

                // then
                assertThat(response.getSuccessCount()).isGreaterThan(0);
                assertThat(response.getFailureCount()).isEqualTo(0);
                verify(annualLeaveRepository, atLeastOnce()).save(any());
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
                User admin = createMockUser(Authority.MANAGE_EMPLOYEE_DATA);
                given(userRepository.findById(loginUserId)).willReturn(Optional.of(admin));

                // when & then
                assertThatThrownBy(
                    () ->
                        annualLeaveService.uploadAnnualLeaveFile(
                            invalidFile, sheetName, loginUserId))
                    .isInstanceOf(CustomException.class)
                    .hasMessageContaining(
                        "유효하지 않은 기준일자 형식입니다. (yyyy-MM-dd)"); // ErrorCode 메시지에 따라 수정
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
                    .willReturn(Optional.of(createMockUser(Authority.MANAGE_EMPLOYEE_DATA)));
                given(userRepository.findByNameAndJoinDate(anyString(), any()))
                    .willReturn(Optional.empty()); // 유저 못 찾음

                MultipartFile mockFile = createMockExcelFile();

                // when
                ExcelUploadResponseDto response =
                    annualLeaveService.uploadAnnualLeaveFile(mockFile, sheetName, loginUserId);

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

                User admin = createMockUser(Authority.MANAGE_EMPLOYEE_DATA);
                given(userRepository.findById(loginUserId)).willReturn(Optional.of(admin));

                given(mockFile.getInputStream()).willThrow(new IOException("강제 발생 에러"));

                // when & then
                assertThatThrownBy(
                    () ->
                        annualLeaveService.uploadAnnualLeaveFile(
                            mockFile, sheetName, loginUserId))
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
}
