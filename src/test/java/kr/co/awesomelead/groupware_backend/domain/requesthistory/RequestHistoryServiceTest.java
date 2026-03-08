package kr.co.awesomelead.groupware_backend.domain.requesthistory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import kr.co.awesomelead.groupware_backend.domain.requesthistory.dto.request.RequestHistoryCreateRequestDto;
import kr.co.awesomelead.groupware_backend.domain.requesthistory.dto.response.AdminRequestHistorySummaryResponseDto;
import kr.co.awesomelead.groupware_backend.domain.requesthistory.dto.response.RequestHistoryDetailResponseDto;
import kr.co.awesomelead.groupware_backend.domain.requesthistory.dto.response.RequestHistorySummaryResponseDto;
import kr.co.awesomelead.groupware_backend.domain.requesthistory.entity.RequestHistory;
import kr.co.awesomelead.groupware_backend.domain.requesthistory.enums.RequestHistoryStatus;
import kr.co.awesomelead.groupware_backend.domain.requesthistory.enums.RequestType;
import kr.co.awesomelead.groupware_backend.domain.requesthistory.repository.RequestHistoryRepository;
import kr.co.awesomelead.groupware_backend.domain.requesthistory.service.RequestHistoryService;
import kr.co.awesomelead.groupware_backend.domain.user.entity.User;
import kr.co.awesomelead.groupware_backend.domain.user.enums.Position;
import kr.co.awesomelead.groupware_backend.domain.user.enums.Role;
import kr.co.awesomelead.groupware_backend.domain.user.repository.UserRepository;
import kr.co.awesomelead.groupware_backend.global.error.CustomException;
import kr.co.awesomelead.groupware_backend.global.error.ErrorCode;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@ExtendWith(MockitoExtension.class)
@DisplayName("RequestHistoryService 단위 테스트")
class RequestHistoryServiceTest {

    @InjectMocks private RequestHistoryService requestHistoryService;
    @Mock private RequestHistoryRepository requestHistoryRepository;
    @Mock private UserRepository userRepository;

    @Nested
    @DisplayName("createRequest 메서드는")
    class Describe_createRequest {

        @Test
        @DisplayName("정상 입력이면 신청 내역을 생성한다")
        void it_creates_request() {
            // given
            User user = User.builder().nameKor("홍길동").position(Position.STAFF).build();
            ReflectionTestUtils.setField(user, "id", 1L);

            RequestHistoryCreateRequestDto dto = new RequestHistoryCreateRequestDto();
            ReflectionTestUtils.setField(dto, "requestType", RequestType.EMPLOYMENT_CERTIFICATE);
            ReflectionTestUtils.setField(dto, "purpose", "은행 제출용");
            ReflectionTestUtils.setField(dto, "copies", 1);
            ReflectionTestUtils.setField(dto, "wishDate", LocalDate.of(2026, 3, 10));

            RequestHistory saved = new RequestHistory();
            ReflectionTestUtils.setField(saved, "id", 100L);

            given(userRepository.findById(1L)).willReturn(Optional.of(user));
            given(requestHistoryRepository.save(any(RequestHistory.class))).willReturn(saved);

            // when
            Long result = requestHistoryService.createRequest(1L, dto);

            // then
            assertThat(result).isEqualTo(100L);
        }
    }

    @Nested
    @DisplayName("getMyRequests 메서드는")
    class Describe_getMyRequests {

        @Test
        @DisplayName("내 신청 목록을 반환한다")
        void it_returns_my_requests() {
            // given
            User user = new User();
            ReflectionTestUtils.setField(user, "id", 1L);
            RequestHistory requestHistory = new RequestHistory();
            ReflectionTestUtils.setField(requestHistory, "id", 10L);
            ReflectionTestUtils.setField(
                    requestHistory, "requestType", RequestType.CAREER_CERTIFICATE);
            ReflectionTestUtils.setField(requestHistory, "purpose", "관공서 제출용");
            ReflectionTestUtils.setField(requestHistory, "copies", 2);

            given(userRepository.findById(1L)).willReturn(Optional.of(user));
            given(requestHistoryRepository.findByUserIdOrderByRequestDateDescIdDesc(1L))
                    .willReturn(List.of(requestHistory));

            // when
            List<RequestHistorySummaryResponseDto> result = requestHistoryService.getMyRequests(1L);

            // then
            assertThat(result).hasSize(1);
            assertThat(result.get(0).getId()).isEqualTo(10L);
        }
    }

    @Nested
    @DisplayName("getMyRequestDetail 메서드는")
    class Describe_getMyRequestDetail {

        @Test
        @DisplayName("본인 요청 상세를 조회한다")
        void it_returns_detail() {
            // given
            User user = new User();
            ReflectionTestUtils.setField(user, "id", 1L);
            RequestHistory requestHistory = new RequestHistory();
            ReflectionTestUtils.setField(requestHistory, "id", 10L);
            ReflectionTestUtils.setField(requestHistory, "name", "홍길동");

            given(userRepository.findById(1L)).willReturn(Optional.of(user));
            given(requestHistoryRepository.findByIdAndUserId(10L, 1L))
                    .willReturn(Optional.of(requestHistory));

            // when
            RequestHistoryDetailResponseDto result =
                    requestHistoryService.getMyRequestDetail(1L, 10L);

            // then
            assertThat(result.getId()).isEqualTo(10L);
            assertThat(result.getName()).isEqualTo("홍길동");
        }

        @Test
        @DisplayName("본인 요청이 아니면 REQUEST_HISTORY_NOT_FOUND 예외를 던진다")
        void it_throws_when_not_my_request() {
            // given
            User user = new User();
            ReflectionTestUtils.setField(user, "id", 1L);

            given(userRepository.findById(1L)).willReturn(Optional.of(user));
            given(requestHistoryRepository.findByIdAndUserId(10L, 1L)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> requestHistoryService.getMyRequestDetail(1L, 10L))
                    .isInstanceOf(CustomException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.REQUEST_HISTORY_NOT_FOUND);
        }
    }

    @Nested
    @DisplayName("cancelMyRequest 메서드는")
    class Describe_cancelMyRequest {

        @Test
        @DisplayName("대기 상태 본인 요청을 취소한다")
        void it_cancels_waiting_request() {
            // given
            User user = new User();
            ReflectionTestUtils.setField(user, "id", 1L);
            RequestHistory requestHistory = new RequestHistory();
            ReflectionTestUtils.setField(
                    requestHistory, "approvalStatus", RequestHistoryStatus.PENDING);

            given(userRepository.findById(1L)).willReturn(Optional.of(user));
            given(requestHistoryRepository.findByIdAndUserId(10L, 1L))
                    .willReturn(Optional.of(requestHistory));

            // when
            requestHistoryService.cancelMyRequest(1L, 10L);

            // then
            assertThat(requestHistory.getApprovalStatus()).isEqualTo(RequestHistoryStatus.CANCELED);
            verify(requestHistoryRepository).findByIdAndUserId(10L, 1L);
        }

        @Test
        @DisplayName("대기 상태가 아니면 REQUEST_HISTORY_NOT_CANCELABLE 예외를 던진다")
        void it_throws_when_not_waiting_status() {
            // given
            User user = new User();
            ReflectionTestUtils.setField(user, "id", 1L);
            RequestHistory requestHistory = new RequestHistory();
            ReflectionTestUtils.setField(
                    requestHistory, "approvalStatus", RequestHistoryStatus.ISSUED);

            given(userRepository.findById(1L)).willReturn(Optional.of(user));
            given(requestHistoryRepository.findByIdAndUserId(10L, 1L))
                    .willReturn(Optional.of(requestHistory));

            // when & then
            assertThatThrownBy(() -> requestHistoryService.cancelMyRequest(1L, 10L))
                    .isInstanceOf(CustomException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.REQUEST_HISTORY_NOT_CANCELABLE);
        }
    }

    @Nested
    @DisplayName("getAllRequestsForAdmin 메서드는")
    class Describe_getAllRequestsForAdmin {

        @Test
        @DisplayName("관리자 권한이면 전체 신청 목록을 반환한다")
        void it_returns_all_requests_for_admin() {
            // given
            User admin = new User();
            ReflectionTestUtils.setField(admin, "id", 100L);
            ReflectionTestUtils.setField(admin, "role", Role.ADMIN);

            RequestHistory requestHistory = new RequestHistory();
            ReflectionTestUtils.setField(requestHistory, "id", 10L);
            ReflectionTestUtils.setField(requestHistory, "name", "홍길동");

            PageRequest pageable = PageRequest.of(0, 20);
            Page<RequestHistory> page = new PageImpl<>(List.of(requestHistory), pageable, 1);

            given(userRepository.findById(100L)).willReturn(Optional.of(admin));
            given(
                            requestHistoryRepository.findAllWithUserAndDepartmentByStatus(
                                    RequestHistoryStatus.PENDING, pageable))
                    .willReturn(page);

            // when
            Page<AdminRequestHistorySummaryResponseDto> result =
                    requestHistoryService.getAllRequestsForAdmin(
                            100L, RequestHistoryStatus.PENDING, pageable);

            // then
            assertThat(result.getTotalElements()).isEqualTo(1);
            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getContent().get(0).getRequestId()).isEqualTo(10L);
        }

        @Test
        @DisplayName("관리자 권한이 아니면 NO_AUTHORITY_FOR_REGISTRATION 예외를 던진다")
        void it_throws_when_not_admin() {
            // given
            User normalUser = new User();
            ReflectionTestUtils.setField(normalUser, "id", 200L);
            ReflectionTestUtils.setField(normalUser, "role", Role.USER);

            given(userRepository.findById(200L)).willReturn(Optional.of(normalUser));

            // when & then
            assertThatThrownBy(
                            () ->
                                    requestHistoryService.getAllRequestsForAdmin(
                                            200L, null, PageRequest.of(0, 20)))
                    .isInstanceOf(CustomException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.NO_AUTHORITY_FOR_REGISTRATION);
        }
    }
}
