package kr.co.awesomelead.groupware_backend.domain.visit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import kr.co.awesomelead.groupware_backend.domain.user.entity.User;
import kr.co.awesomelead.groupware_backend.domain.user.repository.UserRepository;
import kr.co.awesomelead.groupware_backend.domain.visit.dto.request.CompanionRequestDto;
import kr.co.awesomelead.groupware_backend.domain.visit.dto.request.OnSiteVisitCreateRequestDto;
import kr.co.awesomelead.groupware_backend.domain.visit.dto.request.VisitorRequestDto;
import kr.co.awesomelead.groupware_backend.domain.visit.dto.response.VisitResponseDto;
import kr.co.awesomelead.groupware_backend.domain.visit.entity.Visit;
import kr.co.awesomelead.groupware_backend.domain.visit.entity.Visitor;
import kr.co.awesomelead.groupware_backend.domain.visit.enums.VisitPurpose;
import kr.co.awesomelead.groupware_backend.domain.visit.enums.VisitType;
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

@ExtendWith(MockitoExtension.class)
@ActiveProfiles("test")
public class VisitServiceTest {

    @Mock
    private VisitRepository visitRepository;
    @Mock
    private VisitorRepository visitorRepository;
    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private VisitService visitService;

    @Test
    @DisplayName("현장 방문 등록 성공 테스트 - 동행자 포함")
    void createOnSiteVisit_Success() {
        // given
        Long hostId = 1L;
        OnSiteVisitCreateRequestDto requestDto = createRequestDto(hostId);

        User host = new User();
        ReflectionTestUtils.setField(host, "id", hostId);
        ReflectionTestUtils.setField(host, "nameKor", "담당자");

        Visitor visitor = new Visitor();
        ReflectionTestUtils.setField(visitor, "id", 10L);
        ReflectionTestUtils.setField(visitor, "name", "방문객");

        // Mock 설정
        when(userRepository.findById(hostId)).thenReturn(Optional.of(host));
        when(visitorRepository.findByPhoneNumber(requestDto.getVisitor().getPhoneNumber()))
            .thenReturn(Optional.of(visitor));

        when(visitRepository.save(any(Visit.class))).thenAnswer(invocation -> {
            Visit visit = invocation.getArgument(0);
            ReflectionTestUtils.setField(visit, "id", 100L); // 저장된 ID 모킹
            return visit;
        });

        // when
        VisitResponseDto response = visitService.createOnSiteVisit(requestDto);

        // then
        assertThat(response).isNotNull();
        assertThat(response.getVisitType()).isEqualTo(VisitType.ON_SITE);

        ArgumentCaptor<Visit> visitCaptor = ArgumentCaptor.forClass(Visit.class);
        verify(visitRepository, times(1)).save(visitCaptor.capture());

        Visit savedVisit = visitCaptor.getValue();
        assertThat(savedVisit.isVisited()).isTrue(); // 진형님 로직: 초기값은 false
        assertThat(savedVisit.getHostCompany()).isEqualTo("어썸리드");
        assertThat(savedVisit.getCompanions().size()).isEqualTo(1);
        assertThat(savedVisit.getCompanions().get(0).getName()).isEqualTo("동행자1");
    }

    @Test
    @DisplayName("현장 방문 등록 실패 - 담당 직원이 없는 경우")
    void createOnSiteVisit_Fail_UserNotFound() {
        // given
        OnSiteVisitCreateRequestDto requestDto = createRequestDto(99L);
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        // when & then
        CustomException exception = assertThrows(CustomException.class,
            () -> visitService.createOnSiteVisit(requestDto));

        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.USER_NOT_FOUND);
        verify(visitRepository, never()).save(any());
    }

    // 테스트 데이터 생성 헬퍼 메서드
    private OnSiteVisitCreateRequestDto createRequestDto(Long hostId) {
        OnSiteVisitCreateRequestDto dto = new OnSiteVisitCreateRequestDto();
        dto.setHostUserId(hostId);
        dto.setHostCompany("어썸리드");
        dto.setPurpose(VisitPurpose.MEETING);
        dto.setVisitStartDate(LocalDateTime.now().plusHours(1));
        dto.setVisitEndDate(LocalDateTime.now().plusHours(3));

        VisitorRequestDto visitorDto = new VisitorRequestDto();
        visitorDto.setName("방문객");
        visitorDto.setPhoneNumber("01012345678");
        visitorDto.setPassword("1234");
        visitorDto.setVisitorCompany("외부업체");
        dto.setVisitor(visitorDto);

        CompanionRequestDto companionDto = new CompanionRequestDto();
        companionDto.setName("동행자1");
        companionDto.setPhoneNumber("01099998888");
        companionDto.setVisitorCompany("외부업체");
        dto.setCompanions(List.of(companionDto));

        return dto;
    }

}
