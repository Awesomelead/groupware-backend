package kr.co.awesomelead.groupware_backend.domain.notice;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import kr.co.awesomelead.groupware_backend.domain.department.dto.response.UserSummaryResponseDto;
import kr.co.awesomelead.groupware_backend.domain.department.entity.Department;
import kr.co.awesomelead.groupware_backend.domain.department.enums.Company;
import kr.co.awesomelead.groupware_backend.domain.department.service.DepartmentService;
import kr.co.awesomelead.groupware_backend.domain.notice.dto.request.NoticeCreateRequestDto;
import kr.co.awesomelead.groupware_backend.domain.notice.dto.request.NoticeSearchConditionDto;
import kr.co.awesomelead.groupware_backend.domain.notice.dto.request.NoticeUpdateRequestDto;
import kr.co.awesomelead.groupware_backend.domain.notice.dto.response.NoticeDetailDto;
import kr.co.awesomelead.groupware_backend.domain.notice.dto.response.NoticeSummaryDto;
import kr.co.awesomelead.groupware_backend.domain.notice.entity.Notice;
import kr.co.awesomelead.groupware_backend.domain.notice.entity.NoticeAttachment;
import kr.co.awesomelead.groupware_backend.domain.notice.mapper.NoticeMapper;
import kr.co.awesomelead.groupware_backend.domain.notice.respository.NoticeAttachmentRepository;
import kr.co.awesomelead.groupware_backend.domain.notice.respository.NoticeQueryRepository;
import kr.co.awesomelead.groupware_backend.domain.notice.respository.NoticeRepository;
import kr.co.awesomelead.groupware_backend.domain.notice.respository.NoticeTargetRepository;
import kr.co.awesomelead.groupware_backend.domain.notice.service.NoticeService;
import kr.co.awesomelead.groupware_backend.domain.user.entity.User;
import kr.co.awesomelead.groupware_backend.domain.user.enums.Authority;
import kr.co.awesomelead.groupware_backend.domain.user.repository.UserRepository;
import kr.co.awesomelead.groupware_backend.global.error.CustomException;
import kr.co.awesomelead.groupware_backend.global.error.ErrorCode;
import kr.co.awesomelead.groupware_backend.global.infra.s3.S3Service;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
@DisplayName("NoticeService 단위 테스트")
class NoticeServiceTest {

    @InjectMocks
    private NoticeService noticeService;

    @Mock
    private NoticeRepository noticeRepository;
    @Mock
    private NoticeQueryRepository noticeQueryRepository;
    @Mock
    private NoticeAttachmentRepository noticeAttachmentRepository;
    @Mock
    private NoticeTargetRepository noticeTargetRepository;
    @Mock
    private NoticeMapper noticeMapper;
    @Mock
    private S3Service s3Service;
    @Mock
    private UserRepository userRepository;
    @Mock
    private DepartmentService departmentService;

    private User adminUser;
    private User regularUser;
    private Department testDepartment;

    @BeforeEach
    void setUp() {
        testDepartment = new Department();
        ReflectionTestUtils.setField(testDepartment, "company", Company.AWESOME);

        adminUser = new User();
        ReflectionTestUtils.setField(adminUser, "id", 1L);
        adminUser.addAuthority(Authority.ACCESS_NOTICE);
        ReflectionTestUtils.setField(adminUser, "department", testDepartment);

        regularUser = new User();
        ReflectionTestUtils.setField(regularUser, "id", 2L);
        ReflectionTestUtils.setField(regularUser, "department", testDepartment);
    }

    @Nested
    @DisplayName("createNotice 메서드는")
    class Describe_createNotice {

        @Nested
        @DisplayName("유효한 작성 권한과 파일이 주어지면")
        class Context_with_valid_authority {

            @Test
            @DisplayName("회사/부서/개인 타겟을 모두 취합하여 공지 대상을 생성한다")
            void it_creates_notice_with_flattened_targets() throws IOException {
                // given
                NoticeCreateRequestDto dto = NoticeCreateRequestDto.builder()
                    .title("제목")
                    .targetCompanies(List.of(Company.AWESOME))
                    .targetDepartmentIds(List.of(10L))
                    .targetUserIds(List.of(99L))
                    .build();

                Notice notice = Notice.builder().build();
                ReflectionTestUtils.setField(notice, "id", 100L);

                given(userRepository.findById(1L)).willReturn(Optional.of(adminUser));
                given(noticeMapper.toNoticeEntity(any(), any())).willReturn(notice);
                given(noticeRepository.save(any())).willReturn(notice);

                given(userRepository.findAllIdsByCompany(Company.AWESOME)).willReturn(
                    List.of(1L, 2L));
                UserSummaryResponseDto deptUser = new UserSummaryResponseDto();
                ReflectionTestUtils.setField(deptUser, "id", 3L);
                given(departmentService.getUsersByDepartmentHierarchy(10L)).willReturn(
                    List.of(deptUser));

                // when
                Long resultId = noticeService.createNotice(dto, null, 1L);

                // then
                assertThat(resultId).isEqualTo(100L);
                verify(noticeTargetRepository).saveAll(any());
                verify(noticeRepository, times(1)).save(any());
            }
        }

        @Nested
        @DisplayName("공지사항 작성 권한이 없는 유저가 요청하면")
        class Context_with_no_authority {

            @Test
            @DisplayName("NO_AUTHORITY_FOR_NOTICE 예외를 던진다")
            void it_throws_exception() {
                given(userRepository.findById(2L)).willReturn(Optional.of(regularUser));

                assertThatThrownBy(() -> noticeService.createNotice(null, null, 2L))
                    .isInstanceOf(CustomException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.NO_AUTHORITY_FOR_NOTICE);
            }
        }
    }

    @Nested
    @DisplayName("getNoticesByType 메서드는")
    class Describe_getNoticesByType {

        @Nested
        @DisplayName("관리자 권한을 가진 유저가 조회하면")
        class Context_admin_user {

            @Test
            @DisplayName("권한 여부를 true로 설정하여 모든 공지를 조회한다")
            void it_calls_repo_with_admin_authority() {
                // given
                Pageable pageable = PageRequest.of(0, 10);
                NoticeSearchConditionDto condition = new NoticeSearchConditionDto();
                given(userRepository.findById(1L)).willReturn(Optional.of(adminUser));
                given(noticeQueryRepository.findNoticesWithFilters(eq(condition), eq(1L), eq(true),
                    any()))
                    .willReturn(Page.empty());

                // when
                noticeService.getNoticesByType(condition, 1L, pageable);

                // then
                verify(noticeQueryRepository).findNoticesWithFilters(eq(condition), eq(1L),
                    eq(true), any());
            }
        }

        @Nested
        @DisplayName("일반 유저가 조회하면")
        class Context_regular_user {

            @Test
            @DisplayName("해당 유저의 회사 필터를 적용하여 조회한다")
            void it_calls_repo_with_user_company() {
                // given
                NoticeSearchConditionDto condition = new NoticeSearchConditionDto();
                given(userRepository.findById(2L)).willReturn(Optional.of(regularUser));
                given(noticeQueryRepository.findNoticesWithFilters(eq(condition), eq(2L), eq(false),
                    any()))
                    .willReturn(Page.empty());

                // when
                noticeService.getNoticesByType(condition, 2L, PageRequest.of(0, 10));

                // then
                verify(noticeQueryRepository).findNoticesWithFilters(eq(condition), eq(2L),
                    eq(false), any());
            }
        }
    }

    @Nested
    @DisplayName("getNotice 메서드는")
    class Describe_getNotice {

        @Test
        @DisplayName("조회수를 1 증가시키고 상세 정보를 반환한다")
        void it_increases_view_count_and_returns_dto() {
            // given
            Notice notice = Notice.builder().title("상세조회").build();
            ReflectionTestUtils.setField(notice, "viewCount", 0);
            given(noticeRepository.findByIdWithDetails(1L)).willReturn(Optional.of(notice));
            given(noticeMapper.toNoticeDetailDto(any(), any())).willReturn(
                NoticeDetailDto.builder().title("상세조회").build());

            // when
            NoticeDetailDto result = noticeService.getNotice(1L);

            // then
            assertThat(notice.getViewCount()).isEqualTo(1);
            verify(noticeRepository).save(notice);
            assertThat(result.getTitle()).isEqualTo("상세조회");
        }
    }

    @Nested
    @DisplayName("updateNotice 메서드는")
    class Describe_updateNotice {

        @Test
        @DisplayName("기존 첨부파일 삭제와 새로운 파일 업로드를 수행한다")
        void it_updates_notice_and_attachments() throws IOException {
            // given
            Notice notice = Notice.builder().build();
            NoticeAttachment oldAttachment = new NoticeAttachment();
            ReflectionTestUtils.setField(oldAttachment, "s3Key", "old-key");

            NoticeUpdateRequestDto dto = NoticeUpdateRequestDto.builder()
                .title("수정제목")
                .attachmentsIdsToRemove(List.of(10L))
                .build();

            given(userRepository.findById(1L)).willReturn(Optional.of(adminUser));
            given(noticeRepository.findByIdWithDetails(1L)).willReturn(Optional.of(notice));
            given(noticeAttachmentRepository.findById(10L)).willReturn(Optional.of(oldAttachment));
            given(s3Service.uploadFile(any())).willReturn("new-key");

            // when
            noticeService.updateNotice(1L, 1L, dto,
                List.of(new MockMultipartFile("new", "new.txt", "text", "c".getBytes())));

            // then
            verify(s3Service).deleteFile("old-key");
            verify(s3Service).uploadFile(any());
            verify(noticeAttachmentRepository).delete(any());
        }
    }

    @Nested
    @DisplayName("getTop3NoticesForHome 메서드는")
    class Describe_getTop3NoticesForHome {

        @Test
        @DisplayName("홈 화면용 상위 3개 공지를 반환한다")
        void it_returns_top3_list() {
            // given
            given(userRepository.findById(1L)).willReturn(Optional.of(adminUser));
            given(noticeQueryRepository.findTop3Notices(eq(1L), eq(true))).willReturn(List.of());
            // when
            List<NoticeSummaryDto> result = noticeService.getTop3NoticesForHome(1L);

            // then
            assertThat(result).isNotNull();
            verify(noticeQueryRepository).findTop3Notices(eq(1L), eq(true));
        }
    }
}
