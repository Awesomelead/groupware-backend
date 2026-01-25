package kr.co.awesomelead.groupware_backend.domain.notice;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import kr.co.awesomelead.groupware_backend.domain.notice.dto.request.NoticeCreateRequestDto;
import kr.co.awesomelead.groupware_backend.domain.notice.dto.request.NoticeUpdateRequestDto;
import kr.co.awesomelead.groupware_backend.domain.notice.dto.response.NoticeSummaryDto;
import kr.co.awesomelead.groupware_backend.domain.notice.entity.Notice;
import kr.co.awesomelead.groupware_backend.domain.notice.entity.NoticeAttachment;
import kr.co.awesomelead.groupware_backend.domain.notice.enums.NoticeType;
import kr.co.awesomelead.groupware_backend.domain.notice.mapper.NoticeMapper;
import kr.co.awesomelead.groupware_backend.domain.notice.respository.NoticeAttachmentRepository;
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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.multipart.MultipartFile;

@ExtendWith(MockitoExtension.class)
public class NoticeServiceTest {

    @Mock
    private NoticeRepository noticeRepository;
    @Mock
    private NoticeAttachmentRepository noticeAttachmentRepository;
    @Mock
    private NoticeMapper noticeMapper;
    @Mock
    private S3Service s3Service;
    @Mock
    private UserRepository userRepository;
    @InjectMocks
    private NoticeService noticeService;

    private User author;
    private NoticeCreateRequestDto createRequestDto;
    private MockMultipartFile mockFile;

    @BeforeEach
    void setUp() {
        // 1. 공지사항 권한이 있는 테스트용 사용자 생성
        author = new User();
        ReflectionTestUtils.setField(author, "id", 1L);
        ReflectionTestUtils.setField(author, "nameKor", "김공지");
        author.addAuthority(Authority.ACCESS_NOTICE);

        // 2. 테스트용 DTO 생성
        createRequestDto =
            NoticeCreateRequestDto.builder()
                .title("테스트 공지사항")
                .content("공지사항 내용입니다.")
                .type(NoticeType.REGULAR)
                .pinned(false)
                .build();

        // 3. 테스트용 파일 생성
        mockFile =
            new MockMultipartFile(
                "files", "test-image.png", "image/png", "test content".getBytes());
    }

    @Test
    @DisplayName("공지사항 생성 성공 - 첨부파일 포함")
    void createNotice_Success() throws IOException {
        // given
        List<MultipartFile> files = List.of(mockFile);
        Notice notice = new Notice();

        when(userRepository.findById(anyLong())).thenReturn(Optional.of(author));
        when(noticeMapper.toNoticeEntity(any())).thenReturn(notice);
        when(s3Service.uploadFile(any())).thenReturn("s3-key-123");
        when(noticeRepository.save(any(Notice.class)))
            .thenAnswer(
                invocation -> {
                    Notice savedNotice = invocation.getArgument(0);
                    ReflectionTestUtils.setField(savedNotice, "id", 100L); // 저장 후 ID 부여 모사
                    return savedNotice;
                });

        // when
        Long savedId = noticeService.createNotice(createRequestDto, files, 1L);

        // then
        assertThat(savedId).isEqualTo(100L);
        verify(s3Service, times(1)).uploadFile(any()); // S3 업로드 호출 확인
        verify(noticeRepository, times(1)).save(any()); // DB 저장 호출 확인
        assertThat(notice.getAttachments().size()).isEqualTo(1); // 연관관계 편의 메서드 동작 확인
    }

    @Test
    @DisplayName("공지사항 생성 실패 - 유저가 없는 경우")
    void createNotice_Fail_UserNotFound() {
        // given
        Long invalidUserId = 999L;
        when(userRepository.findById(invalidUserId)).thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> noticeService.createNotice(createRequestDto, null, invalidUserId))
            .isInstanceOf(CustomException.class)
            .hasFieldOrPropertyWithValue("errorCode", ErrorCode.USER_NOT_FOUND);

        verify(noticeRepository, never()).save(any());
    }

    @Test
    @DisplayName("공지사항 생성 실패 - 권한이 없는 경우")
    void createNotice_Fail_NoAuthority() throws IOException {
        // given
        Long userId = 1L;
        User userWithoutAuth = new User();
        ReflectionTestUtils.setField(userWithoutAuth, "id", userId);

        when(userRepository.findById(userId)).thenReturn(Optional.of(userWithoutAuth));

        // when & then
        assertThatThrownBy(() -> noticeService.createNotice(createRequestDto, null, userId))
            .isInstanceOf(CustomException.class)
            .hasFieldOrPropertyWithValue("errorCode", ErrorCode.NO_AUTHORITY_FOR_NOTICE);

        verify(s3Service, never()).uploadFile(any());
        verify(noticeRepository, never()).save(any());
    }

    @Test
    @DisplayName("공지사항 목록 조회 성공 - 타입이 null이면 전체 조회")
    void getNoticesByType_All_Success() {
        // given
        List<Notice> notices = List.of(new Notice(), new Notice());
        when(noticeRepository.findAllByOrderByPinnedDescUpdatedDateDesc()).thenReturn(notices);
        when(noticeMapper.toNoticeSummaryDtoList(any()))
            .thenReturn(List.of(new NoticeSummaryDto(), new NoticeSummaryDto()));

        // when
        List<NoticeSummaryDto> result = noticeService.getNoticesByType(null);

        // then
        assertThat(result.size()).isEqualTo(2);
        verify(noticeRepository, times(1)).findAllByOrderByPinnedDescUpdatedDateDesc();
    }

    @Test
    @DisplayName("공지사항 목록 조회 성공 - 특정 타입 조회")
    void getNoticesByType_Filtered_Success() {
        // given
        when(noticeRepository.findByTypeOrderByPinnedDescUpdatedDateDesc(NoticeType.MENU))
            .thenReturn(List.of(new Notice()));

        // when
        noticeService.getNoticesByType(NoticeType.MENU);

        // then
        verify(noticeRepository, times(1))
            .findByTypeOrderByPinnedDescUpdatedDateDesc(NoticeType.MENU);
    }

    @Test
    @DisplayName("공지사항 상세 조회 성공 - 조회수 증가 확인")
    void getNotice_Success() {
        // given
        Notice notice = new Notice();
        ReflectionTestUtils.setField(notice, "viewCount", 10);
        when(noticeRepository.findByIdWithDetails(anyLong())).thenReturn(Optional.of(notice));

        // when
        noticeService.getNotice(1L);

        // then
        assertThat(notice.getViewCount()).isEqualTo(11); // 조회수 증가 검증
        verify(noticeRepository, times(1)).save(notice); // 변경사항 저장 확인
    }

    @Test
    @DisplayName("공지사항 상세 조회 실패 - 공지사항이 없는 경우")
    void getNotice_Fail_NotFound() {
        // given
        when(noticeRepository.findByIdWithDetails(anyLong())).thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> noticeService.getNotice(999L))
            .isInstanceOf(CustomException.class)
            .hasFieldOrPropertyWithValue("errorCode", ErrorCode.NOTICE_NOT_FOUND);
    }

    @Test
    @DisplayName("공지사항 삭제 성공 - S3 파일 삭제 및 DB 삭제 호출")
    void deleteNotice_Success() {
        // given
        Notice notice = new Notice();
        NoticeAttachment attachment = new NoticeAttachment();
        attachment.setS3Key("s3-key-to-delete");
        notice.addAttachment(attachment);

        when(userRepository.findById(anyLong())).thenReturn(Optional.of(author));
        when(noticeRepository.findByIdWithDetails(anyLong())).thenReturn(Optional.of(notice));

        // when
        noticeService.deleteNotice(1L, 10L);

        // then
        verify(s3Service, times(1)).deleteFile("s3-key-to-delete");
        verify(noticeRepository, times(1)).delete(notice);
    }

    @Test
    @DisplayName("공지사항 수정 성공 - 텍스트 수정 및 첨부파일 교체")
    void updateNotice_Success() throws IOException {
        // given
        Long userId = 1L;
        Long noticeId = 10L;

        Notice notice = new Notice();
        ReflectionTestUtils.setField(notice, "title", "이전 제목");

        NoticeAttachment oldAttachment = new NoticeAttachment();
        ReflectionTestUtils.setField(oldAttachment, "id", 50L);
        oldAttachment.setS3Key("old-s3-key");
        notice.addAttachment(oldAttachment);

        NoticeUpdateRequestDto updateDto =
            NoticeUpdateRequestDto.builder()
                .title("수정된 제목")
                .attachmentsIdsToRemove(List.of(50L))
                .build();

        when(userRepository.findById(userId)).thenReturn(Optional.of(author));
        when(noticeRepository.findByIdWithDetails(noticeId)).thenReturn(Optional.of(notice));
        when(noticeAttachmentRepository.findById(50L)).thenReturn(Optional.of(oldAttachment));
        when(s3Service.uploadFile(any())).thenReturn("new-s3-key");

        // when
        Long resultId = noticeService.updateNotice(userId, noticeId, updateDto, List.of(mockFile));

        // then
        assertThat(resultId).isEqualTo(noticeId);
        assertThat(notice.getTitle()).isEqualTo("수정된 제목");
        verify(s3Service).deleteFile("old-s3-key");
        verify(s3Service).uploadFile(any());
        assertThat(notice.getAttachments().size()).isEqualTo(1);
    }

    @Test
    @DisplayName("공지사항 수정 실패 - 삭제하려는 첨부파일이 없는 경우")
    void updateNotice_Fail_AttachmentNotFound() {
        // given
        Long userId = 1L;
        Long noticeId = 10L;

        Notice notice = new Notice();
        NoticeUpdateRequestDto updateDto =
            NoticeUpdateRequestDto.builder().attachmentsIdsToRemove(List.of(99L)).build();

        when(userRepository.findById(userId)).thenReturn(Optional.of(author));
        when(noticeRepository.findByIdWithDetails(noticeId)).thenReturn(Optional.of(notice));
        when(noticeAttachmentRepository.findById(99L)).thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> noticeService.updateNotice(userId, noticeId, updateDto, null))
            .isInstanceOf(CustomException.class)
            .hasFieldOrPropertyWithValue("errorCode", ErrorCode.NOTICE_ATTACHMENT_NOT_FOUND);
    }
}
