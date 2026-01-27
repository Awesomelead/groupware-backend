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
import kr.co.awesomelead.groupware_backend.domain.department.entity.Department;
import kr.co.awesomelead.groupware_backend.domain.department.enums.Company;
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
import org.springframework.web.multipart.MultipartFile;

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
    private NoticeMapper noticeMapper;
    @Mock
    private S3Service s3Service;
    @Mock
    private UserRepository userRepository;

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
            @DisplayName("공지사항을 생성하고 ID를 반환한다")
            void it_returns_notice_id() throws IOException {
                // given
                NoticeCreateRequestDto dto = NoticeCreateRequestDto.builder().title("제목").build();
                List<MultipartFile> files = List.of(
                    new MockMultipartFile("file", "test.txt", "text/plain", "content".getBytes()));
                Notice notice = Notice.builder().build();
                ReflectionTestUtils.setField(notice, "id", 100L);

                given(userRepository.findById(1L)).willReturn(Optional.of(adminUser));
                given(noticeMapper.toNoticeEntity(any(), any())).willReturn(notice);
                given(s3Service.uploadFile(any())).willReturn("s3-key");
                given(noticeRepository.save(any())).willReturn(notice);

                // when
                Long resultId = noticeService.createNotice(dto, files, 1L);

                // then
                assertThat(resultId).isEqualTo(100L);
                verify(s3Service, times(1)).uploadFile(any());
                verify(noticeRepository).save(any());
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
            @DisplayName("회사 필터 없이(null) 모든 공지를 조회한다")
            void it_calls_repo_with_null_company() {
                // given
                Pageable pageable = PageRequest.of(0, 10);
                given(userRepository.findById(1L)).willReturn(Optional.of(adminUser));
                given(noticeQueryRepository.findNoticesWithFilters(any(), eq(null), any()))
                    .willReturn(Page.empty());

                // when
                noticeService.getNoticesByType(new NoticeSearchConditionDto(), 1L, pageable);

                // then
                verify(noticeQueryRepository).findNoticesWithFilters(any(), eq(null), any());
            }
        }

        @Nested
        @DisplayName("일반 유저가 조회하면")
        class Context_regular_user {

            @Test
            @DisplayName("해당 유저의 회사 필터를 적용하여 조회한다")
            void it_calls_repo_with_user_company() {
                // given
                given(userRepository.findById(2L)).willReturn(Optional.of(regularUser));
                given(
                    noticeQueryRepository.findNoticesWithFilters(any(), eq(Company.AWESOME), any()))
                    .willReturn(Page.empty());

                // when
                noticeService.getNoticesByType(new NoticeSearchConditionDto(), 2L,
                    PageRequest.of(0, 10));

                // then
                verify(noticeQueryRepository).findNoticesWithFilters(any(), eq(Company.AWESOME),
                    any());
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
            given(noticeQueryRepository.findTop3Notices(null)).willReturn(List.of());

            // when
            List<NoticeSummaryDto> result = noticeService.getTop3NoticesForHome(1L);

            // then
            assertThat(result).isNotNull();
            verify(noticeQueryRepository).findTop3Notices(null);
        }
    }
}
