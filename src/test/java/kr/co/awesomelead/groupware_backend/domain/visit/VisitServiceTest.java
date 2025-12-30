package kr.co.awesomelead.groupware_backend.domain.visit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import kr.co.awesomelead.groupware_backend.domain.user.entity.User;
import kr.co.awesomelead.groupware_backend.domain.user.repository.UserRepository;
import kr.co.awesomelead.groupware_backend.domain.visit.dto.request.CompanionRequestDto;
import kr.co.awesomelead.groupware_backend.domain.visit.dto.request.VisitCreateRequestDto;
import kr.co.awesomelead.groupware_backend.domain.visit.dto.request.VisitSearchRequestDto;
import kr.co.awesomelead.groupware_backend.domain.visit.dto.response.VisitResponseDto;
import kr.co.awesomelead.groupware_backend.domain.visit.entity.Companion;
import kr.co.awesomelead.groupware_backend.domain.visit.entity.Visit;
import kr.co.awesomelead.groupware_backend.domain.visit.entity.Visitor;
import kr.co.awesomelead.groupware_backend.domain.visit.enums.VisitPurpose;
import kr.co.awesomelead.groupware_backend.domain.visit.enums.VisitType;
import kr.co.awesomelead.groupware_backend.domain.visit.mapper.VisitMapper;
import kr.co.awesomelead.groupware_backend.domain.visit.repository.VisitRepository;
import kr.co.awesomelead.groupware_backend.domain.visit.repository.VisitorRepository;
import kr.co.awesomelead.groupware_backend.domain.visit.service.VisitService;
import kr.co.awesomelead.groupware_backend.global.CustomException;
import kr.co.awesomelead.groupware_backend.global.ErrorCode;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@ExtendWith(MockitoExtension.class)
@ActiveProfiles("test")
public class VisitServiceTest {

    @Mock private VisitRepository visitRepository;
    @Mock private VisitorRepository visitorRepository;
    @Mock private UserRepository userRepository;
    @InjectMocks private VisitService visitService;
    @Mock private VisitMapper visitMapper;

    @Test
    @DisplayName("현장 방문 등록 성공 테스트 - 동행자 포함")
    void createOnSiteVisit_Success() {
        // given
        Long hostId = 1L;
        VisitCreateRequestDto requestDto = createRequestDto(hostId);

        User host = new User();
        ReflectionTestUtils.setField(host, "id", hostId);

        Visitor visitor = new Visitor();
        ReflectionTestUtils.setField(visitor, "id", 10L);
        ReflectionTestUtils.setField(visitor, "phoneNumber", requestDto.getVisitorPhone());

        Visit visit =
                Visit.builder()
                        .user(host)
                        .visitor(visitor)
                        .hostCompany(requestDto.getHostCompany())
                        .visitorCompany(requestDto.getVisitorCompany())
                        .visitType(VisitType.ON_SITE)
                        .visited(true)
                        .verified(true)
                        .build();
        // 동행자 수동 추가 (매퍼 동작 모킹용)
        visit.addCompanion(Companion.builder().name("동행자1").build());

        // Mock 설정
        when(userRepository.findById(hostId)).thenReturn(Optional.of(host));
        when(visitorRepository.findByPhoneNumber(requestDto.getVisitorPhone()))
                .thenReturn(Optional.of(visitor));
        when(visitMapper.toVisitEntity(any(), any(), any(), any())).thenReturn(visit);
        VisitResponseDto mockResponse =
                VisitResponseDto.builder()
                        .id(100L) // 실제 서비스 로직이 반환할 데이터와 유사하게 세팅
                        .visitorName(requestDto.getVisitorName())
                        .build();
        when(visitMapper.toResponseDto(any())).thenReturn(mockResponse);

        when(visitRepository.save(any(Visit.class)))
                .thenAnswer(
                        invocation -> {
                            Visit v = invocation.getArgument(0);
                            ReflectionTestUtils.setField(v, "id", 100L);
                            return v;
                        });

        // when
        VisitResponseDto response = visitService.createOnSiteVisit(requestDto);

        // then
        assertThat(response).isNotNull();
        verify(visitRepository, times(1)).save(any(Visit.class));

        ArgumentCaptor<Visit> visitCaptor = ArgumentCaptor.forClass(Visit.class);
        verify(visitRepository).save(visitCaptor.capture());

        Visit savedVisit = visitCaptor.getValue();
        assertThat(savedVisit.isVisited()).isTrue();
        assertThat(savedVisit.getCompanions().get(0).getVisit()).isEqualTo(savedVisit);
    }

    @Test
    @DisplayName("현장 방문 등록 실패 - 담당 직원이 없는 경우")
    void createOnSiteVisit_Fail_UserNotFound() {
        // given
        VisitCreateRequestDto requestDto = createRequestDto(99L);
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        // when & then
        CustomException exception =
                assertThrows(
                        CustomException.class, () -> visitService.createOnSiteVisit(requestDto));

        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.USER_NOT_FOUND);
        verify(visitRepository, never()).save(any());
    }

    @Test
    @DisplayName("사전 방문 예약 성공 - 비밀번호 포함")
    void createPreVisit_Success() {
        // given
        VisitCreateRequestDto requestDto = createRequestDto(1L);
        requestDto.setVisitorPassword("1234"); // 비밀번호 설정

        User host = new User();
        Visitor visitor = new Visitor();
        Visit visit = Visit.builder().build();

        when(userRepository.findById(any())).thenReturn(Optional.of(host));
        when(visitorRepository.findByPhoneNumber(any())).thenReturn(Optional.of(visitor));
        when(visitMapper.toVisitEntity(any(), any(), any(), any())).thenReturn(visit);
        when(visitRepository.save(any())).thenReturn(visit);

        // when
        visitService.createPreVisit(requestDto);

        // then
        verify(visitRepository, times(1)).save(any());
    }

    @Test
    @DisplayName("사전 방문 예약 실패 - 비밀번호 누락")
    void createPreVisit_Fail_NoPassword() {
        // given
        VisitCreateRequestDto requestDto = createRequestDto(1L);
        requestDto.setVisitorPassword(""); // 비밀번호 비움

        // when & then
        CustomException exception =
                assertThrows(CustomException.class, () -> visitService.createPreVisit(requestDto));

        assertThat(exception.getErrorCode())
                .isEqualTo(ErrorCode.VISITOR_PASSWORD_REQUIRED_FOR_PRE_REGISTRATION);
    }

    @Test
    @DisplayName("내방객 리스트 조회 성공")
    void getMyVisits_Success() {
        // given
        VisitSearchRequestDto searchDto = new VisitSearchRequestDto("방문객", "01012345678", "1234");
        Visitor visitor = new Visitor();
        ReflectionTestUtils.setField(visitor, "name", "방문객");
        ReflectionTestUtils.setField(visitor, "password", "1234");

        when(visitorRepository.findByPhoneNumber(searchDto.getPhoneNumber()))
                .thenReturn(Optional.of(visitor));
        when(visitRepository.findByVisitor(visitor)).thenReturn(List.of(Visit.builder().build()));
        // when
        visitService.getMyVisits(searchDto);

        // then
        verify(visitMapper).toVisitSummaryResponseDtoList(any());
    }

    @Test
    @DisplayName("내방객 리스트 조회 실패 - 인증 정보 불일치")
    void getMyVisits_Fail_Authentication() {
        // given
        VisitSearchRequestDto searchDto =
                new VisitSearchRequestDto("방문객", "01012345678", "wrong_pw");
        Visitor visitor = new Visitor();
        ReflectionTestUtils.setField(visitor, "name", "방문객");
        ReflectionTestUtils.setField(visitor, "password", "1234"); // 실제 비번은 1234

        when(visitorRepository.findByPhoneNumber(any())).thenReturn(Optional.of(visitor));

        // when & then
        CustomException exception =
                assertThrows(CustomException.class, () -> visitService.getMyVisits(searchDto));

        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.VISITOR_AUTHENTICATION_FAILED);
    }

    @Test
    @DisplayName("현장 체크인 성공 - 방문 시작 시간 갱신 확인")
    void checkIn_Success() {
        // given
        Long visitId = 100L;
        Visit visit = Visit.builder().build(); // 초기 상태
        when(visitRepository.findById(visitId)).thenReturn(Optional.of(visit));

        // when
        visitService.checkIn(visitId);

        // then
        // 💡 엔티티 내부의 checkIn()이 호출되어 visitStartDate가 채워졌는지 검증
        assertThat(visit.getVisitStartDate()).isNotNull();
    }

    @Test
    @DisplayName("퇴실 처리 성공")
    void checkOut_Success() {
        // given
        Long visitId = 100L;
        Visit visit = Visit.builder().build();
        when(visitRepository.findById(visitId)).thenReturn(Optional.of(visit));

        // when
        visitService.checkOut(visitId);

        // then
        assertThat(visit.getVisitEndDate()).isNotNull();
    }

    @Test
    @DisplayName("퇴실 처리 실패 - 이미 퇴실한 경우")
    void checkOut_Fail_AlreadyCheckedOut() {
        // given
        Long visitId = 100L;
        Visit visit = Visit.builder().build();
        ReflectionTestUtils.setField(visit, "visitEndDate", LocalDateTime.now()); // 이미 퇴실함

        when(visitRepository.findById(visitId)).thenReturn(Optional.of(visit));

        // when & then
        CustomException exception =
                assertThrows(CustomException.class, () -> visitService.checkOut(visitId));

        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.VISIT_ALREADY_CHECKED_OUT);
    }

    // 테스트 데이터 생성 헬퍼 메서드
    private VisitCreateRequestDto createRequestDto(Long hostId) {
        VisitCreateRequestDto dto = new VisitCreateRequestDto();
        dto.setHostUserId(hostId);
        dto.setHostCompany("어썸리드");
        dto.setVisitorName("방문객");
        dto.setVisitorPhone("01012345678");
        dto.setVisitorPassword("1234");
        dto.setVisitorCompany("외부업체");
        dto.setPurpose(VisitPurpose.MEETING);
        dto.setVisitStartDate(LocalDateTime.now().plusHours(1));

        CompanionRequestDto companionDto = new CompanionRequestDto();
        companionDto.setName("동행자1");
        companionDto.setPhoneNumber("01099998888");
        companionDto.setVisitorCompany("외부업체");
        dto.setCompanions(List.of(companionDto));

        return dto;
    }
}
