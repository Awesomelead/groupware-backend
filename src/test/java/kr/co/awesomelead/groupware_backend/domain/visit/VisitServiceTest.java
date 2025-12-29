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
