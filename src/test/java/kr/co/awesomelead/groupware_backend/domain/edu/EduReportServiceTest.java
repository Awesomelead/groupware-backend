package kr.co.awesomelead.groupware_backend.domain.edu;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import kr.co.awesomelead.groupware_backend.domain.department.entity.Department;
import kr.co.awesomelead.groupware_backend.domain.department.enums.DepartmentName;
import kr.co.awesomelead.groupware_backend.domain.department.repository.DepartmentRepository;
import kr.co.awesomelead.groupware_backend.domain.education.dto.request.EduReportRequestDto;
import kr.co.awesomelead.groupware_backend.domain.education.dto.request.EduReportStatusUpdateRequestDto;
import kr.co.awesomelead.groupware_backend.domain.education.dto.request.EduReportUpdateRequestDto;
import kr.co.awesomelead.groupware_backend.domain.education.dto.response.EduReportDetailDto;
import kr.co.awesomelead.groupware_backend.domain.education.dto.response.EduReportSummaryDto;
import kr.co.awesomelead.groupware_backend.domain.education.entity.EduAttachment;
import kr.co.awesomelead.groupware_backend.domain.education.entity.EduAttendance;
import kr.co.awesomelead.groupware_backend.domain.education.entity.EduReport;
import kr.co.awesomelead.groupware_backend.domain.education.entity.EducationCategory;
import kr.co.awesomelead.groupware_backend.domain.education.enums.EduReportStatus;
import kr.co.awesomelead.groupware_backend.domain.education.enums.EduType;
import kr.co.awesomelead.groupware_backend.domain.education.enums.EducationCategoryType;
import kr.co.awesomelead.groupware_backend.domain.education.mapper.EduMapper;
import kr.co.awesomelead.groupware_backend.domain.education.repository.EduAttachmentRepository;
import kr.co.awesomelead.groupware_backend.domain.education.repository.EduAttendanceRepository;
import kr.co.awesomelead.groupware_backend.domain.education.repository.EduReportQueryRepository;
import kr.co.awesomelead.groupware_backend.domain.education.repository.EduReportRepository;
import kr.co.awesomelead.groupware_backend.domain.education.repository.EducationCategoryRepository;
import kr.co.awesomelead.groupware_backend.domain.education.service.EduReportService;
import kr.co.awesomelead.groupware_backend.domain.notification.service.NotificationService;
import kr.co.awesomelead.groupware_backend.domain.user.entity.User;
import kr.co.awesomelead.groupware_backend.domain.user.enums.Authority;
import kr.co.awesomelead.groupware_backend.domain.user.enums.Role;
import kr.co.awesomelead.groupware_backend.domain.user.enums.Status;
import kr.co.awesomelead.groupware_backend.domain.user.repository.UserRepository;
import kr.co.awesomelead.groupware_backend.global.error.CustomException;
import kr.co.awesomelead.groupware_backend.global.error.ErrorCode;
import kr.co.awesomelead.groupware_backend.global.infra.s3.service.S3Service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@ExtendWith(MockitoExtension.class)
@ActiveProfiles("test")
public class EduReportServiceTest {

    @Mock private EduReportRepository eduReportRepository;
    @Mock private EduAttendanceRepository eduAttendanceRepository;
    @Mock private EduAttachmentRepository eduAttachmentRepository;
    @Mock private EducationCategoryRepository educationCategoryRepository;
    @Mock private EduMapper eduMapper;
    @Mock private DepartmentRepository departmentRepository;
    @Mock private UserRepository userRepository;
    @Mock private S3Service s3Service;
    @Mock private EduReportQueryRepository eduReportQueryRepository;

    @Mock private NotificationService notificationService;

    @InjectMocks private EduReportService eduReportService;

    private Department defaultDept;

    @BeforeEach
    void setUp() {
        defaultDept = Department.builder().id(1L).name(DepartmentName.SALES_DEPT).build();
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
                        .eduType(EduType.SAFETY)
                        .categoryId(1L)
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
                        .title(requestDto.getTitle())
                        .content(requestDto.getContent())
                        .pinned(false) // 기본값
                        .signatureRequired(false) // 기본값
                        .department(null) // SAFETY 교육이므로 null
                        .attachments(attachments)
                        .build();

        User user = createNormalUser();
        user.addAuthority(Authority.MANAGE_SAFETY);
        EducationCategory category =
                EducationCategory.builder()
                        .id(1L)
                        .categoryType(EducationCategoryType.SAFETY)
                        .build();

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(educationCategoryRepository.findById(1L)).thenReturn(Optional.of(category));
        when(eduMapper.toEduReportEntity(any(EduReportRequestDto.class), any(), any()))
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

        verify(notificationService, times(1))
                .sendEduReportAlertToTargets(
                        anyString(), anyString(), anyLong(), any(), any(Map.class));
    }

    @Test
    @DisplayName("교육 게시물 생성 시 파일명이 없는 files 파트는 첨부파일로 저장하지 않음")
    void createEduReport_IgnoresPlaceholderFilePart() throws IOException {
        // given
        EduReportRequestDto requestDto =
                EduReportRequestDto.builder()
                        .title("교육 제목")
                        .content("교육 내용")
                        .eduType(EduType.SAFETY)
                        .categoryId(1L)
                        .build();

        MultipartFile placeholderFile = org.mockito.Mockito.mock(MultipartFile.class);
        when(placeholderFile.isEmpty()).thenReturn(false);
        when(placeholderFile.getOriginalFilename()).thenReturn(null);

        EduReport report =
                EduReport.builder()
                        .id(1L)
                        .eduType(EduType.SAFETY)
                        .title("교육 제목")
                        .content("교육 내용")
                        .build();

        User user = createNormalUser();
        user.addAuthority(Authority.MANAGE_SAFETY);
        EducationCategory category =
                EducationCategory.builder()
                        .id(1L)
                        .categoryType(EducationCategoryType.SAFETY)
                        .build();

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(educationCategoryRepository.findById(1L)).thenReturn(Optional.of(category));
        when(eduMapper.toEduReportEntity(any(EduReportRequestDto.class), any(), any()))
                .thenReturn(report);
        when(eduReportRepository.save(report)).thenReturn(report);

        // when
        eduReportService.createEduReport(requestDto, List.of(placeholderFile), 1L);

        // then
        verify(s3Service, never()).uploadFile(any(MultipartFile.class));
        assertThat(report.getAttachments().size()).isEqualTo(0);
    }

    @Test
    @DisplayName("교육 게시물 생성 시 swagger placeholder(blob:string) files 파트는 첨부파일로 저장하지 않음")
    void createEduReport_IgnoresSwaggerBlobStringPlaceholder() throws IOException {
        // given
        EduReportRequestDto requestDto =
                EduReportRequestDto.builder()
                        .title("교육 제목")
                        .content("교육 내용")
                        .eduType(EduType.SAFETY)
                        .categoryId(1L)
                        .build();

        MultipartFile placeholderFile =
                new MockMultipartFile(
                        "files",
                        "blob",
                        "application/octet-stream",
                        "string".getBytes(StandardCharsets.UTF_8));

        EduReport report =
                EduReport.builder()
                        .id(1L)
                        .eduType(EduType.SAFETY)
                        .title("교육 제목")
                        .content("교육 내용")
                        .build();

        User user = createNormalUser();
        user.addAuthority(Authority.MANAGE_SAFETY);
        EducationCategory category =
                EducationCategory.builder()
                        .id(1L)
                        .categoryType(EducationCategoryType.SAFETY)
                        .build();

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(educationCategoryRepository.findById(1L)).thenReturn(Optional.of(category));
        when(eduMapper.toEduReportEntity(any(EduReportRequestDto.class), any(), any()))
                .thenReturn(report);
        when(eduReportRepository.save(report)).thenReturn(report);

        // when
        eduReportService.createEduReport(requestDto, List.of(placeholderFile), 1L);

        // then
        verify(s3Service, never()).uploadFile(any(MultipartFile.class));
        assertThat(report.getAttachments().size()).isEqualTo(0);
    }

    @Test
    @DisplayName("교육 보고서 생성 실패 - 유저가 없는 경우")
    void createEduReport_Fail_UserNotFound() {
        // given
        EduReportRequestDto requestDto =
                EduReportRequestDto.builder()
                        .title("교육 보고서 제목")
                        .content("교육 보고서 내용")
                        .eduType(EduType.SAFETY)
                        .categoryId(1L)
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
    @DisplayName("교육 보고서 생성 실패 - 안전보건 작성 권한이 없는 경우")
    void createEduReport_Fail_NO_AUTHORITY_FOR_EDU_REPORT() {
        // given
        EduReportRequestDto requestDto =
                EduReportRequestDto.builder()
                        .title("교육 보고서 제목")
                        .content("교육 보고서 내용")
                        .eduType(EduType.SAFETY)
                        .categoryId(1L)
                        .departmentId(null) // 안전교육이므로 부서 아이디 제외
                        .build();

        User user = createNormalUser(); // 권한이 없는 일반 유저

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        // when & then
        assertThatThrownBy(() -> eduReportService.createEduReport(requestDto, null, 1L))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode") // CustomException 내부의 errorCode 필드 추출
                .isEqualTo(ErrorCode.NO_AUTHORITY_FOR_SAFETY_WRITE);

        verify(eduReportRepository, never()).save(any());
    }

    @Test
    @DisplayName("교육 보고서 생성 실패 - PSM은 MANAGE_PSM 권한이 없는 경우")
    void createEduReport_Fail_NO_AUTHORITY_FOR_PSM_MANAGE() {
        // given
        EduReportRequestDto requestDto =
                EduReportRequestDto.builder()
                        .title("PSM 교육 제목")
                        .content("PSM 교육 내용")
                        .eduType(EduType.PSM)
                        .categoryId(1L)
                        .build();

        User user = createNormalUser();
        user.addAuthority(Authority.MANAGE_SAFETY);

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        // when & then
        assertThatThrownBy(() -> eduReportService.createEduReport(requestDto, null, 1L))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.NO_AUTHORITY_FOR_PSM_MANAGE);

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
                        .eduType(EduType.DEPARTMENT)
                        .departmentId(999L) // 존재하지 않는 부서 아이디
                        .build();

        User user = createNormalUser();
        user.addAuthority(Authority.MANAGE_DEPARTMENT_EDUCATION);

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
    @DisplayName("교육 보고서 목록 조회 성공 - 권한 없는 유저 (자신의 부서만 조회)")
    void getEduReports_Success() {
        // given
        User user = createNormalUser(); // MANAGE_DEPARTMENT_EDUCATION 권한 없음
        Department department = defaultDept;

        EduReportSummaryDto report1 =
                EduReportSummaryDto.builder()
                        .id(1L)
                        .title("안전 교육 보고서")
                        .eduType(EduType.SAFETY)
                        .eduDate(LocalDate.now())
                        .content("안전 교육 보고서 내용")
                        .attendance(true)
                        .pinned(false)
                        .build();

        List<EduReportSummaryDto> mockList = List.of(report1);

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(eduReportQueryRepository.findEduReports(EduType.SAFETY, department, null, 1L, false))
                .thenReturn(mockList);

        // when
        List<EduReportSummaryDto> result =
                eduReportService.getEduReports(EduType.SAFETY, null, null, 1L);

        // then
        assertThat(result).isNotNull();
        assertThat(result.size()).isEqualTo(1);
        assertThat(result.get(0).getTitle()).isEqualTo("안전 교육 보고서");

        verify(eduReportQueryRepository, times(1))
                .findEduReports(EduType.SAFETY, department, null, 1L, false);
    }

    @Test
    @DisplayName("교육 보고서 목록 조회 성공 - MANAGE_DEPARTMENT_EDUCATION 권한 있음, 부서 미지정 → 전체 조회")
    void getEduReports_WithAccess_ReturnsAll() {
        // given
        User user = createNormalUser();
        user.addAuthority(Authority.MANAGE_DEPARTMENT_EDUCATION);

        EduReportSummaryDto report1 =
                EduReportSummaryDto.builder()
                        .id(1L)
                        .title("전체 공개 안전 교육")
                        .eduType(EduType.SAFETY)
                        .eduDate(LocalDate.now())
                        .content("전체 공개 안전 교육 내용")
                        .attendance(false)
                        .pinned(false)
                        .build();

        List<EduReportSummaryDto> mockList = List.of(report1);

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        // dept=null → 전체 조회
        when(eduReportQueryRepository.findEduReports(null, null, null, 1L, true))
                .thenReturn(mockList);

        // when
        List<EduReportSummaryDto> result = eduReportService.getEduReports(null, null, null, 1L);

        // then
        assertThat(result).isNotNull();
        assertThat(result.size()).isEqualTo(1);

        // hasAccess=true, dept=null → QueryRepository에 null 부서로 호출
        verify(eduReportQueryRepository, times(1)).findEduReports(null, null, null, 1L, true);
    }

    @Test
    @DisplayName("교육 보고서 목록 조회 성공 - MANAGE_DEPARTMENT_EDUCATION 권한 있음, 특정 부서 필터")
    void getEduReports_WithAccess_FilterByDept() {
        // given
        User user = createNormalUser();
        user.addAuthority(Authority.MANAGE_DEPARTMENT_EDUCATION);

        Department salesDept = Department.builder().id(2L).name(DepartmentName.SALES_DEPT).build();

        EduReportSummaryDto report1 =
                EduReportSummaryDto.builder()
                        .id(2L)
                        .title("영업부 부서 교육")
                        .eduType(EduType.DEPARTMENT)
                        .eduDate(LocalDate.now())
                        .content("영업부 부서 교육 내용")
                        .attendance(false)
                        .pinned(false)
                        .build();

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(departmentRepository.findByName(DepartmentName.SALES_DEPT))
                .thenReturn(Optional.of(salesDept));
        when(eduReportQueryRepository.findEduReports(EduType.DEPARTMENT, salesDept, null, 1L, true))
                .thenReturn(List.of(report1));

        // when
        List<EduReportSummaryDto> result =
                eduReportService.getEduReports(
                        EduType.DEPARTMENT, DepartmentName.SALES_DEPT, null, 1L);

        // then
        assertThat(result).isNotNull();
        assertThat(result.size()).isEqualTo(1);
        assertThat(result.get(0).getTitle()).isEqualTo("영업부 부서 교육");

        verify(departmentRepository, times(1)).findByName(DepartmentName.SALES_DEPT);
        verify(eduReportQueryRepository, times(1))
                .findEduReports(EduType.DEPARTMENT, salesDept, null, 1L, true);
    }

    @Test
    @DisplayName("교육 보고서 목록 조회 실패 - 유저가 없는 경우")
    void getEduReports_Fail_UserNotFound() {
        // given
        when(userRepository.findById(1L)).thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> eduReportService.getEduReports(EduType.SAFETY, null, null, 1L))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.USER_NOT_FOUND);

        verify(eduReportQueryRepository, never())
                .findEduReports(any(), any(), any(), anyLong(), anyBoolean());
    }

    @Test
    @DisplayName("교육 보고서 조회 성공테스트 - 권한 없는 유저 (attendees=null)")
    void getEduReport_Success() {
        // given
        Long reportId = 1L;
        Long userId = 99L;
        User user = createNormalUser(); // MANAGE_DEPARTMENT_EDUCATION 권한 없음

        EduReport report = EduReport.builder().id(reportId).title("단일 조회 테스트 제목").build();

        EduReportDetailDto mockDto =
                EduReportDetailDto.builder().id(reportId).title("단일 조회 테스트 제목").build();

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(eduReportRepository.findById(reportId)).thenReturn(Optional.of(report));
        // 권한 없음 → attendances=null, numberOfPeople=-1L
        when(eduMapper.toDetailDto(report, null, -1L, s3Service)).thenReturn(mockDto);
        when(eduAttendanceRepository.existsByEduReportAndUser(report, user)).thenReturn(true);

        // when
        EduReportDetailDto result = eduReportService.getEduReport(reportId, userId);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getTitle()).isEqualTo("단일 조회 테스트 제목");
        assertThat(result.isAttendance()).isTrue();
        assertThat(result.getAttendees()).isNull(); // 권한 없으면 null

        verify(eduReportRepository, times(1)).findById(reportId);
        verify(eduAttendanceRepository, times(1)).existsByEduReportAndUser(report, user);
        // 출석자 목록 조회는 호출되지 않아야 함
        verify(eduAttendanceRepository, never()).findAllByEduReportIdWithUser(anyLong());
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
        adminUser.addAuthority(Authority.MANAGE_DEPARTMENT_EDUCATION);

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
        adminUser.addAuthority(Authority.MANAGE_DEPARTMENT_EDUCATION);

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
    @DisplayName("교육 보고서 조회 - MANAGE_DEPARTMENT_EDUCATION 권한 있음 → attendees 포함")
    void getEduReport_WithAccess_IncludesAttendees() {
        // given
        Long reportId = 1L;
        Long userId = 99L;
        User user = createNormalUser();
        user.addAuthority(Authority.MANAGE_DEPARTMENT_EDUCATION);

        EduReport report =
                EduReport.builder().id(reportId).title("권한 보고서").eduType(EduType.SAFETY).build();

        List<EduAttendance> attendances = new ArrayList<>();
        long totalCount = 30L;

        EduReportDetailDto mockDto =
                EduReportDetailDto.builder()
                        .id(reportId)
                        .title("권한 보고서")
                        .numberOfPeople(30)
                        .numberOfAttendees(0)
                        .attendees(List.of())
                        .build();

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(eduReportRepository.findById(reportId)).thenReturn(Optional.of(report));
        when(eduAttendanceRepository.findAllByEduReportIdWithUser(reportId))
                .thenReturn(attendances);
        when(userRepository.count()).thenReturn(totalCount);
        when(eduMapper.toDetailDto(report, attendances, totalCount, s3Service)).thenReturn(mockDto);
        when(eduAttendanceRepository.existsByEduReportAndUser(report, user)).thenReturn(false);

        // when
        EduReportDetailDto result = eduReportService.getEduReport(reportId, userId);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getAttendees()).isNotNull(); // 권한 있으면 attendees 포함
        assertThat(result.getNumberOfPeople()).isEqualTo(30);

        verify(eduAttendanceRepository, times(1)).findAllByEduReportIdWithUser(reportId);
    }

    @Test
    @DisplayName("교육 보고서 조회 - MANAGE_DEPARTMENT_EDUCATION 권한 없음 → attendees=null")
    void getEduReport_WithoutAccess_AttendeesIsNull() {
        // given
        Long reportId = 1L;
        Long userId = 1L;
        User user = createNormalUser(); // 권한 없음

        EduReport report =
                EduReport.builder().id(reportId).title("일반 보고서").eduType(EduType.SAFETY).build();

        EduReportDetailDto mockDto =
                EduReportDetailDto.builder()
                        .id(reportId)
                        .title("일반 보고서")
                        .attendees(null)
                        .numberOfPeople(null)
                        .build();

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(eduReportRepository.findById(reportId)).thenReturn(Optional.of(report));
        when(eduMapper.toDetailDto(report, null, -1L, s3Service)).thenReturn(mockDto);
        when(eduAttendanceRepository.existsByEduReportAndUser(report, user)).thenReturn(false);

        // when
        EduReportDetailDto result = eduReportService.getEduReport(reportId, userId);

        // then
        assertThat(result.getAttendees()).isNull(); // 권한 없으면 null
        assertThat(result.getNumberOfPeople()).isNull();

        verify(eduAttendanceRepository, never()).findAllByEduReportIdWithUser(anyLong());
    }

    @Test
    @DisplayName("교육 수정 성공 - 안전보건은 MANAGE_SAFETY 권한으로 수정 가능")
    void updateEduReport_Safety_Success() {
        // given
        Long reportId = 10L;
        Long userId = 1L;
        User user = createNormalUser();
        user.addAuthority(Authority.MANAGE_SAFETY);

        EducationCategory oldCategory =
                EducationCategory.builder()
                        .id(1L)
                        .categoryType(EducationCategoryType.SAFETY)
                        .build();
        EducationCategory newCategory =
                EducationCategory.builder()
                        .id(2L)
                        .categoryType(EducationCategoryType.SAFETY)
                        .build();

        EduReport report =
                EduReport.builder()
                        .id(reportId)
                        .eduType(EduType.SAFETY)
                        .title("기존 제목")
                        .content("기존 내용")
                        .pinned(false)
                        .signatureRequired(false)
                        .status(EduReportStatus.OPEN)
                        .category(oldCategory)
                        .build();

        EduReportUpdateRequestDto requestDto =
                EduReportUpdateRequestDto.builder()
                        .title("수정 제목")
                        .content("수정 내용")
                        .pinned(true)
                        .signatureRequired(false)
                        .categoryId(2L)
                        .build();

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(eduReportRepository.findById(reportId)).thenReturn(Optional.of(report));
        when(eduAttendanceRepository.countByEduReportId(reportId)).thenReturn(0L);
        when(educationCategoryRepository.findById(2L)).thenReturn(Optional.of(newCategory));

        // when
        Long updatedId = eduReportService.updateEduReport(reportId, requestDto, userId);

        // then
        assertThat(updatedId).isEqualTo(reportId);
        assertThat(report.getTitle()).isEqualTo("수정 제목");
        assertThat(report.getContent()).isEqualTo("수정 내용");
        assertThat(report.isPinned()).isTrue();
        assertThat(report.getCategory()).isEqualTo(newCategory);
        verify(departmentRepository, never()).findById(anyLong());
    }

    @Test
    @DisplayName("교육 수정 실패 - 안전보건은 MANAGE_SAFETY 권한이 없으면 수정 불가")
    void updateEduReport_Safety_Fail_NoAuthority() {
        // given
        Long reportId = 10L;
        Long userId = 1L;
        User user = createNormalUser();

        EduReport report =
                EduReport.builder()
                        .id(reportId)
                        .eduType(EduType.SAFETY)
                        .status(EduReportStatus.OPEN)
                        .build();

        EduReportUpdateRequestDto requestDto =
                EduReportUpdateRequestDto.builder().title("수정 제목").content("수정 내용").build();

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(eduReportRepository.findById(reportId)).thenReturn(Optional.of(report));

        // when & then
        assertThatThrownBy(() -> eduReportService.updateEduReport(reportId, requestDto, userId))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.NO_AUTHORITY_FOR_SAFETY_WRITE);
        verify(eduAttendanceRepository, never()).countByEduReportId(anyLong());
    }

    @Test
    @DisplayName("교육 수정 실패 - 부서 교육 수정 시 departmentId 필수")
    void updateEduReport_Department_Fail_DepartmentIdRequired() {
        // given
        Long reportId = 10L;
        Long userId = 1L;
        User user = createNormalUser();
        user.addAuthority(Authority.MANAGE_DEPARTMENT_EDUCATION);

        EduReport report =
                EduReport.builder()
                        .id(reportId)
                        .eduType(EduType.DEPARTMENT)
                        .status(EduReportStatus.OPEN)
                        .build();

        EduReportUpdateRequestDto requestDto =
                EduReportUpdateRequestDto.builder().title("수정 제목").content("수정 내용").build();

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(eduReportRepository.findById(reportId)).thenReturn(Optional.of(report));
        when(eduAttendanceRepository.countByEduReportId(reportId)).thenReturn(0L);

        // when & then
        assertThatThrownBy(() -> eduReportService.updateEduReport(reportId, requestDto, userId))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.DEPARTMENT_ID_REQUIRED);
        verify(departmentRepository, never()).findById(anyLong());
    }

    @Test
    @DisplayName("교육 수정 성공 - 첨부파일 삭제 및 추가")
    void updateEduReport_WithAttachmentChanges_Success() throws IOException {
        // given
        Long reportId = 20L;
        Long userId = 1L;
        User user = createNormalUser();
        user.addAuthority(Authority.MANAGE_DEPARTMENT_EDUCATION);

        EduReport report =
                EduReport.builder()
                        .id(reportId)
                        .eduType(EduType.DEPARTMENT)
                        .status(EduReportStatus.OPEN)
                        .build();

        EduAttachment oldAttachment =
                EduAttachment.builder()
                        .id(100L)
                        .originalFileName("old.pdf")
                        .s3Key("old-s3-key")
                        .fileSize(123L)
                        .build();
        report.addAttachment(oldAttachment);

        MultipartFile newFile = org.mockito.Mockito.mock(MultipartFile.class);
        when(newFile.isEmpty()).thenReturn(false);
        when(newFile.getOriginalFilename()).thenReturn("new.pdf");
        when(newFile.getSize()).thenReturn(456L);

        EduReportUpdateRequestDto requestDto =
                EduReportUpdateRequestDto.builder()
                        .title("수정 제목")
                        .content("수정 내용")
                        .pinned(false)
                        .signatureRequired(false)
                        .departmentId(defaultDept.getId())
                        .deleteAttachmentIds(List.of(100L))
                        .build();

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(eduReportRepository.findById(reportId)).thenReturn(Optional.of(report));
        when(eduAttendanceRepository.countByEduReportId(reportId)).thenReturn(0L);
        when(departmentRepository.findById(defaultDept.getId()))
                .thenReturn(Optional.of(defaultDept));
        when(eduAttachmentRepository.findById(100L)).thenReturn(Optional.of(oldAttachment));
        when(s3Service.uploadFile(newFile)).thenReturn("new-s3-key");

        // when
        Long updatedId =
                eduReportService.updateEduReport(reportId, requestDto, List.of(newFile), userId);

        // then
        assertThat(updatedId).isEqualTo(reportId);
        assertThat(report.getAttachments().size()).isEqualTo(1);
        assertThat(report.getAttachments().get(0).getS3Key()).isEqualTo("new-s3-key");
        verify(s3Service, times(1)).deleteFile("old-s3-key");
        verify(eduAttachmentRepository, times(1)).delete(oldAttachment);
        verify(s3Service, times(1)).uploadFile(newFile);
    }

    @Test
    @DisplayName("교육 수정 실패 - 다른 게시물의 첨부파일 삭제 시도")
    void updateEduReport_Fail_DeleteForeignAttachment() {
        // given
        Long reportId = 21L;
        Long userId = 1L;
        User user = createNormalUser();
        user.addAuthority(Authority.MANAGE_DEPARTMENT_EDUCATION);

        EduReport report =
                EduReport.builder()
                        .id(reportId)
                        .eduType(EduType.DEPARTMENT)
                        .status(EduReportStatus.OPEN)
                        .build();

        EduReport otherReport =
                EduReport.builder()
                        .id(999L)
                        .eduType(EduType.DEPARTMENT)
                        .status(EduReportStatus.OPEN)
                        .build();

        EduAttachment foreignAttachment =
                EduAttachment.builder()
                        .id(777L)
                        .originalFileName("foreign.pdf")
                        .s3Key("foreign-s3-key")
                        .build();
        otherReport.addAttachment(foreignAttachment);

        EduReportUpdateRequestDto requestDto =
                EduReportUpdateRequestDto.builder()
                        .title("수정 제목")
                        .content("수정 내용")
                        .pinned(false)
                        .signatureRequired(false)
                        .departmentId(defaultDept.getId())
                        .deleteAttachmentIds(List.of(777L))
                        .build();

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(eduReportRepository.findById(reportId)).thenReturn(Optional.of(report));
        when(eduAttendanceRepository.countByEduReportId(reportId)).thenReturn(0L);
        when(departmentRepository.findById(defaultDept.getId()))
                .thenReturn(Optional.of(defaultDept));
        when(eduAttachmentRepository.findById(777L)).thenReturn(Optional.of(foreignAttachment));

        // when & then
        assertThatThrownBy(
                        () ->
                                eduReportService.updateEduReport(
                                        reportId, requestDto, List.<MultipartFile>of(), userId))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.EDU_ATTACHMENT_NOT_FOUND);
        verify(s3Service, never()).deleteFile("foreign-s3-key");
    }

    @Test
    @DisplayName("교육 상태 변경 성공 - 안전보건은 MANAGE_SAFETY 권한으로 상태 변경 가능")
    void updateEduReportStatus_Safety_Success() {
        // given
        Long reportId = 11L;
        Long userId = 1L;
        User user = createNormalUser();
        user.addAuthority(Authority.MANAGE_SAFETY);

        EduReport report =
                EduReport.builder()
                        .id(reportId)
                        .eduType(EduType.SAFETY)
                        .status(EduReportStatus.OPEN)
                        .build();

        EduReportStatusUpdateRequestDto requestDto =
                org.mockito.Mockito.mock(EduReportStatusUpdateRequestDto.class);
        when(requestDto.getStatus()).thenReturn(EduReportStatus.CLOSED);

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(eduReportRepository.findById(reportId)).thenReturn(Optional.of(report));

        // when
        Long updatedId = eduReportService.updateEduReportStatus(reportId, requestDto, userId);

        // then
        assertThat(updatedId).isEqualTo(reportId);
        assertThat(report.getStatus()).isEqualTo(EduReportStatus.CLOSED);
    }

    @Test
    @DisplayName("출석 체크 실패 - 마감된 안전보건 교육")
    void markAttendance_Fail_ClosedSafetyReport() {
        // given
        Long reportId = 1L;
        Long userId = 1L;
        User user = createNormalUser();
        EduReport report =
                EduReport.builder()
                        .id(reportId)
                        .eduType(EduType.SAFETY)
                        .status(EduReportStatus.CLOSED)
                        .signatureRequired(false)
                        .build();

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(eduReportRepository.findById(reportId)).thenReturn(Optional.of(report));

        // when & then
        assertThatThrownBy(() -> eduReportService.markAttendance(reportId, null, userId))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.EDU_REPORT_CLOSED);
        verify(eduAttendanceRepository, never()).save(any());
    }
}
