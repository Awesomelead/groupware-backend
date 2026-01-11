package kr.co.awesomelead.groupware_backend.domain.edu;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import kr.co.awesomelead.groupware_backend.domain.department.entity.Department;
import kr.co.awesomelead.groupware_backend.domain.department.repository.DepartmentRepository;
import kr.co.awesomelead.groupware_backend.domain.education.dto.request.EduReportRequestDto;
import kr.co.awesomelead.groupware_backend.domain.education.dto.response.EduReportAdminDetailDto;
import kr.co.awesomelead.groupware_backend.domain.education.dto.response.EduReportDetailDto;
import kr.co.awesomelead.groupware_backend.domain.education.dto.response.EduReportSummaryDto;
import kr.co.awesomelead.groupware_backend.domain.education.entity.EduAttachment;
import kr.co.awesomelead.groupware_backend.domain.education.entity.EduAttendance;
import kr.co.awesomelead.groupware_backend.domain.education.entity.EduReport;
import kr.co.awesomelead.groupware_backend.domain.education.enums.EduType;
import kr.co.awesomelead.groupware_backend.domain.education.mapper.EduMapper;
import kr.co.awesomelead.groupware_backend.domain.education.repository.EduAttachmentRepository;
import kr.co.awesomelead.groupware_backend.domain.education.repository.EduAttendanceRepository;
import kr.co.awesomelead.groupware_backend.domain.education.repository.EduReportRepository;
import kr.co.awesomelead.groupware_backend.domain.education.service.EduReportService;
import kr.co.awesomelead.groupware_backend.domain.user.entity.User;
import kr.co.awesomelead.groupware_backend.domain.user.enums.Authority;
import kr.co.awesomelead.groupware_backend.domain.user.enums.Role;
import kr.co.awesomelead.groupware_backend.domain.user.enums.Status;
import kr.co.awesomelead.groupware_backend.domain.user.repository.UserRepository;
import kr.co.awesomelead.groupware_backend.global.error.CustomException;
import kr.co.awesomelead.groupware_backend.global.error.ErrorCode;
import kr.co.awesomelead.groupware_backend.global.infra.s3.S3Service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@ExtendWith(MockitoExtension.class)
@ActiveProfiles("test")
public class EduReportServiceTest {

    @Mock private EduReportRepository eduReportRepository;
    @Mock private EduAttendanceRepository eduAttendanceRepository;
    @Mock private EduAttachmentRepository eduAttachmentRepository;
    @Mock private EduMapper eduMapper;
    @Mock private DepartmentRepository departmentRepository;
    @Mock private UserRepository userRepository;
    @Mock private S3Service s3Service;

    @InjectMocks private EduReportService eduReportService;

    private Department defaultDept;

    @BeforeEach
    void setUp() {
        defaultDept = Department.builder().id(1L).name("개발팀").build();
    }

    private User createNormalUser() {
        return User.builder()
                .id(1L)
                .nameKor("일반직원")
                .nameEng("Normal User")
                .email("user@awesomelead.co.kr")
                .role(Role.USER)
                .status(Status.AVAILABLE)
                .department(defaultDept)
                .build();
    }

    private User createAdminUser() {
        return User.builder()
                .id(99L)
                .nameKor("관리자")
                .nameEng("Admin User")
                .email("admin@awesomelead.co.kr")
                .role(Role.ADMIN)
                .status(Status.AVAILABLE)
                .build();
    }

    @Test
    @DisplayName("교육 보고서 생성 성공 테스트")
    void createEduReport_Success() throws IOException {
        // given
        EduReportRequestDto requestDto =
                EduReportRequestDto.builder()
                        .title("교육 보고서 제목")
                        .content("교육 보고서 내용")
                        .eduDate(LocalDate.of(2025, 12, 31))
                        .eduType(EduType.SAFETY)
                        .departmentId(null) // 안전교육이므로 부서 아이디 제외
                        .build();

        MultipartFile file = org.mockito.Mockito.mock(MultipartFile.class);
        when(file.getOriginalFilename()).thenReturn("attachment.pdf");
        when(file.getSize()).thenReturn(2048L);

        EduAttachment eduAttachment =
                EduAttachment.builder()
                        .id(1L)
                        .originalFileName("attachment.pdf")
                        .s3Key("uuid-random-string_attachment.pdf")
                        .build();

        ArrayList<EduAttachment> attachments = new ArrayList<>();
        attachments.add(eduAttachment);

        EduReport eduReport =
                EduReport.builder()
                        .id(1L) // Mock 데이터이므로 식별자를 넣어줍니다.
                        .eduType(requestDto.getEduType())
                        .eduDate(requestDto.getEduDate())
                        .title(requestDto.getTitle())
                        .content(requestDto.getContent())
                        .pinned(false) // 기본값
                        .signatureRequired(false) // 기본값
                        .department(null) // SAFETY 교육이므로 null
                        .attachments(attachments)
                        .build();

        User user = createNormalUser();
        user.addAuthority(Authority.WRITE_EDUCATION);

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(eduMapper.toEduReportEntity(any(EduReportRequestDto.class), any()))
                .thenReturn(eduReport);
        when(s3Service.uploadFile(file)).thenReturn(eduAttachment.getS3Key());
        when(eduReportRepository.save(eduReport)).thenReturn(eduReport);

        // when
        eduReportService.createEduReport(
                requestDto,
                new ArrayList<MultipartFile>() {
                    {
                        add(file);
                    }
                },
                1L);

        // then
        verify(s3Service, times(1)).uploadFile(any(MultipartFile.class));

        ArgumentCaptor<EduReport> eduReportCaptor = ArgumentCaptor.forClass(EduReport.class);
        verify(eduReportRepository, times(1)).save(eduReportCaptor.capture());

        EduReport savedReport = eduReportCaptor.getValue();
        assertThat(savedReport.getTitle()).isEqualTo("교육 보고서 제목");
        assertThat(savedReport.getContent()).isEqualTo("교육 보고서 내용");
    }

    @Test
    @DisplayName("교육 보고서 생성 실패 - 유저가 없는 경우")
    void createEduReport_Fail_UserNotFound() {
        // given
        EduReportRequestDto requestDto =
                EduReportRequestDto.builder()
                        .title("교육 보고서 제목")
                        .content("교육 보고서 내용")
                        .eduDate(LocalDate.of(2025, 12, 31))
                        .eduType(EduType.SAFETY)
                        .departmentId(null) // 안전교육이므로 부서 아이디 제외
                        .build();

        when(userRepository.findById(1L)).thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> eduReportService.createEduReport(requestDto, null, 1L))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode") // CustomException 내부의 errorCode 필드 추출
                .isEqualTo(ErrorCode.USER_NOT_FOUND);

        verify(eduReportRepository, never()).save(any());
    }

    @Test
    @DisplayName("교육 보고서 생성 실패 - 권한이 없는 경우")
    void createEduReport_Fail_NO_AUTHORITY_FOR_EDU_REPORT() {
        // given
        EduReportRequestDto requestDto =
                EduReportRequestDto.builder()
                        .title("교육 보고서 제목")
                        .content("교육 보고서 내용")
                        .eduDate(LocalDate.of(2025, 12, 31))
                        .eduType(EduType.SAFETY)
                        .departmentId(null) // 안전교육이므로 부서 아이디 제외
                        .build();

        User user = createNormalUser(); // 권한이 없는 일반 유저

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        // when & then
        assertThatThrownBy(() -> eduReportService.createEduReport(requestDto, null, 1L))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode") // CustomException 내부의 errorCode 필드 추출
                .isEqualTo(ErrorCode.NO_AUTHORITY_FOR_EDU_REPORT);

        verify(eduReportRepository, never()).save(any());
    }

    @Test
    @DisplayName("교육 보고서 생성 실패 - 부서 교육인데 부서가 없는 경우")
    void createEduReport_Fail_DepartmentNotFound() {
        // given
        EduReportRequestDto requestDto =
                EduReportRequestDto.builder()
                        .title("교육 보고서 제목")
                        .content("교육 보고서 내용")
                        .eduDate(LocalDate.of(2025, 12, 31))
                        .eduType(EduType.DEPARTMENT)
                        .departmentId(999L) // 존재하지 않는 부서 아이디
                        .build();

        User user = createNormalUser();
        user.addAuthority(Authority.WRITE_EDUCATION);

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(departmentRepository.findById(999L)).thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> eduReportService.createEduReport(requestDto, null, 1L))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode") // CustomException 내부의 errorCode 필드 추출
                .isEqualTo(ErrorCode.DEPARTMENT_NOT_FOUND);

        verify(eduReportRepository, never()).save(any());
    }

    @Test
    @DisplayName("교육 보고서 목록 조회 성공 테스트")
    void getEduReports_Success() {
        // given
        User user = createNormalUser();
        Department department = defaultDept;

        EduReportSummaryDto report1 =
                EduReportSummaryDto.builder()
                        .id(1L)
                        .title("안전 교육 보고서")
                        .eduType(EduType.SAFETY)
                        .eduDate(LocalDate.now())
                        .attendance(true)
                        .pinned(false)
                        .build();

        List<EduReportSummaryDto> mockList = List.of(report1);

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(eduReportRepository.findEduReportsWithFilters(
                        EduType.SAFETY, user.getDepartment(), user))
                .thenReturn(mockList);

        // when
        List<EduReportSummaryDto> result = eduReportService.getEduReports(EduType.SAFETY, 1L);

        // then
        assertThat(result).isNotNull();
        assertThat(result.size()).isEqualTo(1);
        assertThat(result.get(0).getTitle()).isEqualTo("안전 교육 보고서");

        verify(eduReportRepository, times(1))
                .findEduReportsWithFilters(EduType.SAFETY, department, user);
    }

    @Test
    @DisplayName("교육 보고서 목록 조회 실패 - 유저가 없는 경우")
    void getEduReports_Fail_UserNotFound() {
        // given
        when(userRepository.findById(1L)).thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> eduReportService.getEduReports(EduType.SAFETY, 1L))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode") // CustomException 내부의 errorCode 필드 추출
                .isEqualTo(ErrorCode.USER_NOT_FOUND);

        verify(eduReportRepository, never()).findEduReportsWithFilters(any(), any(), any());
    }

    @Test
    @DisplayName("교육 보고서 조회 성공테스트")
    void getEduReport_Success() {
        // given
        Long reportId = 1L;
        Long userId = 99L;
        User user = createAdminUser();

        EduReport report = EduReport.builder().id(reportId).title("단일 조회 테스트 제목").build();

        EduReportDetailDto mockDto =
                EduReportDetailDto.builder().id(reportId).title("단일 조회 테스트 제목").build();

        when(eduReportRepository.findById(reportId)).thenReturn(Optional.of(report));
        when(eduMapper.toDetailDto(report, s3Service)).thenReturn(mockDto);
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(eduAttendanceRepository.existsByEduReportAndUser(report, user)).thenReturn(true);

        // when
        EduReportDetailDto result = eduReportService.getEduReport(reportId, userId);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getTitle()).isEqualTo("단일 조회 테스트 제목");
        assertThat(result.isAttendance()).isTrue(); // 출석 여부가 true로 세팅되었는지 확인

        verify(eduReportRepository, times(1)).findById(reportId);
        verify(eduAttendanceRepository, times(1)).existsByEduReportAndUser(report, user);
    }

    @Test
    @DisplayName("교육 보고서 단일 조회 실패 - 유저가 존재하지 않음")
    void getEduReport_Fail_UserNotFound() {
        // given
        Long userId = 1L;
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> eduReportService.getEduReport(10L, userId))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.USER_NOT_FOUND);

        // 핵심: 유저가 없으므로 보고서 Repository는 호출되지 않아야 함
        verify(eduReportRepository, never()).findById(anyLong());
    }

    @Test
    @DisplayName("교육 보고서 단일 조회 실패 - 보고서가 존재하지 않음 (2순위 체크)")
    void getEduReport_Fail_EduReportNotFound() {
        // given
        Long userId = 1L;
        Long reportId = 10L;
        User user = createNormalUser();

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(eduReportRepository.findById(reportId)).thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> eduReportService.getEduReport(reportId, userId))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.EDU_REPORT_NOT_FOUND);

        verify(userRepository, times(1)).findById(userId);
        verify(eduAttendanceRepository, never()).existsByEduReportAndUser(any(), any());
    }

    @Test
    @DisplayName("교육 보고서 삭제 성공 테스트")
    void deleteEduReport_Success() throws IOException {
        // given
        Long reportId = 1L;
        Long userId = 99L; // 관리자 유저 아이디
        User adminUser = createAdminUser();
        adminUser.addAuthority(Authority.WRITE_EDUCATION);

        EduAttachment attachment1 =
                EduAttachment.builder()
                        .id(1L)
                        .originalFileName("file1.pdf")
                        .s3Key("s3://bucket/file1.pdf")
                        .build();

        EduAttachment attachment2 =
                EduAttachment.builder()
                        .id(2L)
                        .originalFileName("file2.pdf")
                        .s3Key("s3://bucket/file2.pdf")
                        .build();

        List<EduAttachment> attachments = List.of(attachment1, attachment2);

        EduReport report =
                EduReport.builder()
                        .id(reportId)
                        .title("삭제 테스트 보고서")
                        .attachments(attachments)
                        .build();

        when(userRepository.findById(userId)).thenReturn(Optional.of(adminUser));
        when(eduReportRepository.findById(reportId)).thenReturn(Optional.of(report));

        // when
        eduReportService.deleteEduReport(reportId, userId);

        // then
        verify(s3Service, times(1)).deleteFile("s3://bucket/file1.pdf");
        verify(s3Service, times(1)).deleteFile("s3://bucket/file2.pdf");
        verify(eduReportRepository, times(1)).delete(report);
    }

    @Test
    @DisplayName("교육 보고서 삭제 실패 - 유저가 존재하지 않음")
    void deleteEduReport_Fail_UserNotFound() {
        // given
        Long userId = 1L;
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> eduReportService.deleteEduReport(10L, userId))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.USER_NOT_FOUND);

        verify(eduReportRepository, never()).findById(anyLong());
    }

    @Test
    @DisplayName("교육 보고서 삭제 실패 - 권한이 없는 유저")
    void deleteEduReport_Fail_NoAuthority() {
        // given
        Long userId = 1L;
        User normalUser = createNormalUser(); // 권한 없는 일반 유저

        when(userRepository.findById(userId)).thenReturn(Optional.of(normalUser));

        // when & then
        assertThatThrownBy(() -> eduReportService.deleteEduReport(10L, userId))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.NO_AUTHORITY_FOR_EDU_REPORT);

        verify(eduReportRepository, never()).findById(anyLong());
    }

    @Test
    @DisplayName("교육 보고서 삭제 실패 - 보고서가 존재하지 않음")
    void deleteEduReport_Fail_EduReportNotFound() {
        // given
        Long userId = 99L; // 관리자 유저 아이디
        User adminUser = createAdminUser();
        adminUser.addAuthority(Authority.WRITE_EDUCATION);

        when(userRepository.findById(userId)).thenReturn(Optional.of(adminUser));
        when(eduReportRepository.findById(10L)).thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> eduReportService.deleteEduReport(10L, userId))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.EDU_REPORT_NOT_FOUND);

        verify(eduReportRepository, times(1)).findById(10L);
    }

    @Test
    @DisplayName("첨부파일 다운로드 성공 테스트")
    void getFileForDownload_Success() {
        // given
        Long attachmentId = 1L;
        EduAttachment attachment =
                EduAttachment.builder()
                        .id(attachmentId)
                        .originalFileName("download.pdf")
                        .s3Key("s3://bucket/download.pdf")
                        .fileSize(4096L)
                        .build();

        byte[] fileData = new byte[] {0x25, 0x50, 0x44, 0x46}; // PDF 파일의 일부 바이트 예시

        when(eduAttachmentRepository.findById(attachmentId)).thenReturn(Optional.of(attachment));
        when(s3Service.downloadFile(attachment.getS3Key())).thenReturn(fileData);

        // when
        EduReportService.FileDownloadDto result = eduReportService.getFileForDownload(attachmentId);

        // then
        assertThat(result).isNotNull();
        assertThat(result.originalFileName()).isEqualTo("download.pdf");
        assertThat(result.fileSize()).isEqualTo(4096L);
        assertThat(result.fileData()).isEqualTo(fileData);

        verify(eduAttachmentRepository, times(1)).findById(attachmentId);
        verify(s3Service, times(1)).downloadFile(attachment.getS3Key());
    }

    @Test
    @DisplayName("첨부파일 다운로드 실패 - 첨부파일이 존재하지 않음")
    void getFileForDownload_Fail_EduAttachmentNotFound() {
        // given
        Long attachmentId = 1L;
        when(eduAttachmentRepository.findById(attachmentId)).thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> eduReportService.getFileForDownload(attachmentId))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.EDU_ATTACHMENT_NOT_FOUND);

        verify(s3Service, never()).downloadFile(anyString());
    }

    @Test
    @DisplayName("출석 체크 성공 테스트")
    void markAttendance_Success() throws IOException {
        // given
        Long reportId = 1L;
        Long userId = 1L;

        EduReport report =
                EduReport.builder()
                        .id(reportId)
                        .title("출석 체크 테스트 보고서")
                        .signatureRequired(true)
                        .build();

        User user = createNormalUser();

        MultipartFile signatureFile = org.mockito.Mockito.mock(MultipartFile.class);
        when(signatureFile.getContentType()).thenReturn("image/png");
        when(signatureFile.isEmpty()).thenReturn(false);

        when(eduReportRepository.findById(reportId)).thenReturn(Optional.of(report));
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(eduAttendanceRepository.existsByEduReportAndUser(
                        any(EduReport.class), any(User.class)))
                .thenReturn(false);
        when(s3Service.uploadFile(signatureFile)).thenReturn("s3://bucket/signature.png");

        // when
        eduReportService.markAttendance(reportId, signatureFile, userId);

        // then
        verify(s3Service, times(1)).uploadFile(signatureFile);
        verify(eduAttendanceRepository, times(1)).save(any());
    }

    @Test
    @DisplayName("출석 체크 실패 - 이미 출석 체크한 경우")
    void markAttendance_Fail_AlreadyMarkedAttendance() throws IOException {
        // given
        Long reportId = 1L;
        Long userId = 1L;

        EduReport report =
                EduReport.builder()
                        .id(reportId)
                        .title("출석 체크 테스트 보고서")
                        .signatureRequired(false)
                        .build();

        User user = createNormalUser();

        when(eduReportRepository.findById(reportId)).thenReturn(Optional.of(report));
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(eduAttendanceRepository.existsByEduReportAndUser(report, user)).thenReturn(true);

        // when & then
        assertThatThrownBy(() -> eduReportService.markAttendance(reportId, null, userId))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.ALREADY_MARKED_ATTENDANCE);

        verify(s3Service, never()).uploadFile(any());
        verify(eduAttendanceRepository, never()).save(any());
    }

    @Test
    @DisplayName("출석 체크 실패 - 서명이 필요한데 서명이 제공되지 않은 경우")
    void markAttendance_Fail_NoSignatureProvided() throws IOException {
        // given
        Long reportId = 1L;
        Long userId = 1L;

        // 서명이 필수인 보고서 설정
        EduReport report =
                EduReport.builder().id(reportId).title("서명 필수 보고서").signatureRequired(true).build();

        User user = createNormalUser();

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(eduReportRepository.findById(reportId)).thenReturn(Optional.of(report));
        when(eduAttendanceRepository.existsByEduReportAndUser(report, user)).thenReturn(false);

        // when & then
        assertThatThrownBy(() -> eduReportService.markAttendance(reportId, null, userId))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.NO_SIGNATURE_PROVIDED);

        verify(s3Service, never()).uploadFile(any());
        verify(eduAttendanceRepository, never()).save(any());
    }

    @Test
    @DisplayName("관리자용 교육 보고서 상세 조회 성공 테스트")
    void getEduReportForAdmin_Success() {
        // given
        Long reportId = 1L;
        Long userId = 99L;
        User adminUser = createAdminUser();

        EduReport report =
                EduReport.builder()
                        .id(reportId)
                        .title("관리자용 테스트 보고서")
                        .eduType(EduType.SAFETY)
                        .build();

        List<EduAttendance> attendances = new ArrayList<>();

        int targetCount = 50;
        EduReportAdminDetailDto mockDto =
                EduReportAdminDetailDto.builder()
                        .id(reportId)
                        .title("관리자용 테스트 보고서")
                        .numberOfPeople(targetCount)
                        .numberOfAttendees(0)
                        .build();

        when(userRepository.findById(userId)).thenReturn(Optional.of(adminUser));
        when(eduReportRepository.findById(reportId)).thenReturn(Optional.of(report));
        when(eduAttendanceRepository.findAllByEduReportIdWithUser(reportId))
                .thenReturn(attendances);
        when(userRepository.count()).thenReturn((long) targetCount);
        when(eduMapper.toAdminDetailDto(report, attendances, targetCount, s3Service))
                .thenReturn(mockDto);

        // when
        EduReportAdminDetailDto result = eduReportService.getEduReportForAdmin(reportId, userId);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getTitle()).isEqualTo("관리자용 테스트 보고서");
        assertThat(result.getNumberOfPeople()).isEqualTo(50L);

        verify(userRepository, times(1)).findById(userId);
        verify(eduReportRepository, times(1)).findById(reportId);
    }

    @Test
    @DisplayName("관리자용 상세 조회 실패 - 권한이 없는 유저")
    void getEduReportForAdmin_Fail_NoAuthority() {
        // given
        Long reportId = 1L;
        Long userId = 1L;
        User normalUser = createNormalUser(); // 일반 유저 (ROLE.USER)

        when(userRepository.findById(userId)).thenReturn(Optional.of(normalUser));

        // when & then
        assertThatThrownBy(() -> eduReportService.getEduReportForAdmin(reportId, userId))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.NO_AUTHORITY_FOR_EDU_REPORT);

        verify(userRepository, times(1)).findById(userId);
        verify(eduReportRepository, never()).findById(anyLong());
    }

    @Test
    @DisplayName("관리자용 상세 조회 실패 - 보고서가 존재하지 않음")
    void getEduReportForAdmin_Fail_NotFound() {
        // given
        Long reportId = 1L;
        Long userId = 99L;
        User adminUser = createAdminUser();

        when(userRepository.findById(userId)).thenReturn(Optional.of(adminUser));
        when(eduReportRepository.findById(reportId)).thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> eduReportService.getEduReportForAdmin(reportId, userId))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.EDU_REPORT_NOT_FOUND);

        verify(userRepository, times(1)).findById(userId);
        verify(eduAttendanceRepository, never()).findAllByEduReportIdWithUser(anyLong());
    }
}
