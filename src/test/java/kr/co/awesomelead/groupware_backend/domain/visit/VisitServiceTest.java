package kr.co.awesomelead.groupware_backend.domain.visit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import kr.co.awesomelead.groupware_backend.domain.user.entity.User;
import kr.co.awesomelead.groupware_backend.domain.user.enums.Authority;
import kr.co.awesomelead.groupware_backend.domain.user.enums.JobType;
import kr.co.awesomelead.groupware_backend.domain.user.repository.UserRepository;
import kr.co.awesomelead.groupware_backend.domain.visit.dto.request.CheckInRequestDto;
import kr.co.awesomelead.groupware_backend.domain.visit.dto.request.CheckOutRequestDto;
import kr.co.awesomelead.groupware_backend.domain.visit.dto.request.LongTermVisitRequestDto;
import kr.co.awesomelead.groupware_backend.domain.visit.dto.request.MyVisitDetailRequestDto;
import kr.co.awesomelead.groupware_backend.domain.visit.dto.request.OnSiteVisitRequestDto;
import kr.co.awesomelead.groupware_backend.domain.visit.dto.request.OneDayVisitRequestDto;
import kr.co.awesomelead.groupware_backend.domain.visit.entity.Visit;
import kr.co.awesomelead.groupware_backend.domain.visit.entity.VisitRecord;
import kr.co.awesomelead.groupware_backend.domain.visit.enums.AdditionalPermissionType;
import kr.co.awesomelead.groupware_backend.domain.visit.enums.VisitPurpose;
import kr.co.awesomelead.groupware_backend.domain.visit.enums.VisitStatus;
import kr.co.awesomelead.groupware_backend.domain.visit.mapper.VisitMapper;
import kr.co.awesomelead.groupware_backend.domain.visit.repository.VisitRepository;
import kr.co.awesomelead.groupware_backend.domain.visit.service.VisitService;
import kr.co.awesomelead.groupware_backend.global.error.CustomException;
import kr.co.awesomelead.groupware_backend.global.infra.s3.S3Service;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;

@ExtendWith(MockitoExtension.class)
@ActiveProfiles("test")
public class VisitServiceTest {

    @InjectMocks
    private VisitService visitService;

    @Mock
    private VisitRepository visitRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private VisitMapper visitMapper;
    @Mock
    private S3Service s3Service;
    @Mock
    private PasswordEncoder passwordEncoder;

    @Nested
    @DisplayName("registerOneDayPreVisit 메서드는")
    class Describe_registerOneDayPreVisit {

        @Nested
        @DisplayName("방문목적이 '시설공사'인데, '보충적허가필요여부'가 없으면")
        class Context_with_facility_construction_and_no_additional_permission_type {

            @Test
            @DisplayName("ADDITIONAL_PERMISSION_REQUIRED 예외를 던진다.")
            void it_throws_additional_permission_required_exception() {
                OneDayVisitRequestDto dto = createOneDayDto(
                    VisitPurpose.FACILITY_CONSTRUCTION, null, null);

                assertThatThrownBy(() -> visitService.registerOneDayPreVisit(dto))
                    .isInstanceOf(CustomException.class)
                    .hasMessage("시설공사 목적의 방문 시 추가 허가가 필요합니다.");
            }
        }

        @Nested
        @DisplayName("방문 목적이 '시설공사'이고, 허가 타입이 '기타 허가'인데 상세 내용이 없으면")
        class Context_facility_construction_with_other_permission_but_no_detail {

            @Test
            @DisplayName("PERMISSION_DETAIL_REQUIRED 예외를 던진다.")
            void it_throws_permission_detail_required_exception() {
                OneDayVisitRequestDto dto = createOneDayDto(
                    VisitPurpose.FACILITY_CONSTRUCTION,
                    AdditionalPermissionType.OTHER_PERMISSION,
                    null);

                assertThatThrownBy(() -> visitService.registerOneDayPreVisit(dto))
                    .isInstanceOf(CustomException.class)
                    .hasMessage("기타 허가 선택 시 요구사항 작성이 필요합니다.");
            }
        }

        @Nested
        @DisplayName("정상적인 입력값이 주어지면")
        class Context_with_valid_input {

            @Test
            @DisplayName("방문 예약이 정상적으로 등록된다.")
            void it_registers_visit_successfully() {
                // given
                OneDayVisitRequestDto dto = createOneDayDto(
                    VisitPurpose.MEETING,
                    AdditionalPermissionType.NONE,
                    null);

                User mockHost = User.builder().id(dto.getHostId()).build();
                String encodedPassword = "encoded_password_1234";

                Visit mockVisit = Visit.builder()
                    .id(1L)
                    .visitorName(dto.getVisitorName())
                    .purpose(dto.getPurpose())
                    .user(mockHost)
                    .build();

                given(userRepository.findById(dto.getHostId())).willReturn(Optional.of(mockHost));
                given(passwordEncoder.encode(dto.getPassword())).willReturn(encodedPassword);
                given(visitMapper.toOneDayVisit(any(), any(), any())).willReturn(mockVisit);
                given(visitRepository.save(any(Visit.class))).willReturn(mockVisit);

                // when
                Long resultId = visitService.registerOneDayPreVisit(dto);

                // then
                assertThat(resultId).isEqualTo(1L);
                verify(userRepository, times(1)).findById(dto.getHostId());
                verify(passwordEncoder, times(1)).encode(dto.getPassword());
                verify(visitRepository, times(1)).save(any(Visit.class));
            }
        }

        private OneDayVisitRequestDto createOneDayDto(VisitPurpose purpose,
            AdditionalPermissionType type, String detail) {
            return OneDayVisitRequestDto.builder()
                .visitorName("홍길동")
                .visitorPhoneNumber("01012345678")
                .visitorCompany("테스트컴퍼니")
                .purpose(purpose)
                .permissionType(type)
                .permissionDetail(detail)
                .visitDate(LocalDate.now().plusDays(1))
                .entryTime(LocalTime.of(10, 0))
                .exitTime(LocalTime.of(18, 0))
                .hostId(1L)
                .password("1234")
                .build();
        }
    }

    @Nested
    @DisplayName("registerLongTermPreVisit 메서드는")
    class Describe_registerLongTermPreVisit {

        @Nested
        @DisplayName("날짜 범위가 유효하지 않으면")
        class Context_with_invalid_date_range {

            @Test
            @DisplayName("종료일이 시작일보다 빠를 때 INVALID_VISIT_DATE_RANGE 예외를 던진다.")
            void it_throws_invalid_visit_date_range() {
                // given: 시작일 1월 22일, 종료일 1월 21일 (과거)
                LongTermVisitRequestDto dto = LongTermVisitRequestDto.builder()
                    .startDate(LocalDate.of(2026, 1, 22))
                    .endDate(LocalDate.of(2026, 1, 21))
                    .build();

                // when & then
                assertThatThrownBy(() -> visitService.registerLongTermPreVisit(dto))
                    .isInstanceOf(CustomException.class)
                    .hasMessage("종료일은 시작일보다 빠를 수 없습니다.");
            }

            @Test
            @DisplayName("신청 기간이 3개월을 초과할 때 LONG_TERM_PERIOD_EXCEEDED 예외를 던진다.")
            void it_throws_long_term_period_exceeded() {
                // given: 3개월에서 딱 하루 더 신청 (1/22 ~ 4/23)
                LocalDate startDate = LocalDate.of(2026, 1, 22);
                LongTermVisitRequestDto dto = LongTermVisitRequestDto.builder()
                    .startDate(startDate)
                    .endDate(startDate.plusMonths(3).plusDays(1))
                    .build();

                // when & then
                assertThatThrownBy(() -> visitService.registerLongTermPreVisit(dto))
                    .isInstanceOf(CustomException.class)
                    .hasMessage("장기 방문은 최대 3개월까지만 신청 가능합니다.");
            }
        }

        @Nested
        @DisplayName("날짜 범위가 정확히 3개월이면")
        class Context_with_exact_three_months {

            @Test
            @DisplayName("예외를 던지지 않고 정상 진행한다.")
            void it_proceeds_normally() {
                // given: 1/22 ~ 4/22 (딱 3개월)
                LocalDate startDate = LocalDate.of(2026, 1, 22);
                LongTermVisitRequestDto dto = LongTermVisitRequestDto.builder()
                    .hostId(1L)
                    .password("1234")
                    .startDate(startDate)
                    .endDate(startDate.plusMonths(3))
                    .purpose(VisitPurpose.MEETING)
                    .build();

                // Mocking (정상 저장을 위해 필요한 최소한의 세팅)
                given(userRepository.findById(any())).willReturn(
                    Optional.of(User.builder().id(1L).build()));
                given(passwordEncoder.encode(any())).willReturn("hash");
                given(visitMapper.toLongTermVisit(any(), any(), any())).willReturn(
                    Visit.builder().id(1L).build());
                given(visitRepository.save(any())).willReturn(Visit.builder().id(1L).build());

                // when & then
                assertDoesNotThrow(() -> visitService.registerLongTermPreVisit(dto));
            }
        }
    }

    @Nested
    @DisplayName("registerOnSiteVisit 메서드는")
    class Describe_registerOnSiteVisit {

        @Test
        @DisplayName("현장 방문 신청 시 VisitRecord가 즉시 생성되어 함께 저장된다.")
        void it_creates_visit_with_initial_record() throws IOException {
            // given
            OnSiteVisitRequestDto dto = OnSiteVisitRequestDto.builder()
                .visitorName("현장방문객")
                .hostId(1L)
                .password("1234")
                .signatureFile(
                    new MockMultipartFile("file", "sig.png", "image/png", "test".getBytes()))
                .purpose(VisitPurpose.MEETING)
                .build();

            User mockHost = User.builder().id(1L).build();
            Visit mockVisit = Visit.builder().id(100L).records(new ArrayList<>()).build();

            given(userRepository.findById(any())).willReturn(Optional.of(mockHost));
            given(passwordEncoder.encode(any())).willReturn("hash");
            given(s3Service.uploadFile(any())).willReturn("s3-key-123");
            given(visitMapper.toOnSiteVisit(any(), any(), any())).willReturn(mockVisit);
            given(visitRepository.save(any())).willReturn(mockVisit);

            // when
            visitService.registerOnSiteVisit(dto);

            // then
            verify(visitRepository).save(argThat(visit -> {
                assertThat(visit.getRecords()).hasSize(1);
                assertThat(visit.getRecords().get(0).getSignatureKey()).isEqualTo("s3-key-123");
                return true;
            }));
        }
    }

    @Nested
    @DisplayName("checkIn 메서드는")
    class Describe_checkIn {

        @Mock
        private MockMultipartFile signatureFile;

        @Nested
        @DisplayName("비밀번호가 일치하지 않으면")
        class Context_with_invalid_password {

            @Test
            @DisplayName("INVALID_PASSWORD 예외를 던진다.")
            void it_throws_invalid_password_exception() throws IOException {
                // given
                CheckInRequestDto dto = new CheckInRequestDto(1L, "wrong_pw", signatureFile);
                Visit visit = Visit.builder().password("encoded_pw").build();

                given(visitRepository.findById(1L)).willReturn(Optional.of(visit));
                given(passwordEncoder.matches("wrong_pw", "encoded_pw")).willReturn(false);

                // when & then
                assertThatThrownBy(() -> visitService.checkIn(dto))
                    .isInstanceOf(CustomException.class)
                    .hasMessage("유효하지 않은 비밀번호입니다.");
            }
        }

        @Nested
        @DisplayName("하루 방문인데 방문 예정일이 오늘이 아니면")
        class Context_with_invalid_visit_date {

            @Test
            @DisplayName("NOT_VISIT_DATE 예외를 던진다.")
            void it_throws_not_visit_date_exception() throws IOException {
                // given
                CheckInRequestDto dto = new CheckInRequestDto(1L, "1234", signatureFile);
                // 어제 날짜로 예약된 하루 방문 건
                Visit visit = Visit.builder()
                    .isLongTerm(false)
                    .startDate(LocalDate.now().minusDays(1))
                    .password("encoded_pw")
                    .build();

                given(visitRepository.findById(1L)).willReturn(Optional.of(visit));
                given(passwordEncoder.matches("1234", "encoded_pw")).willReturn(true);

                // when & then
                assertThatThrownBy(() -> visitService.checkIn(dto))
                    .isInstanceOf(CustomException.class)
                    .hasMessage("오늘 방문 일정이 아닙니다.");
            }
        }

        @Nested
        @DisplayName("이미 방문 처리가 완료된 건(visited=true)이라면")
        class Context_already_visited {

            @Test
            @DisplayName("VISIT_ALREADY_CHECKED_OUT 예외를 던진다.")
            void it_throws_visit_already_checked_out_exception() throws IOException {
                // given
                CheckInRequestDto dto = new CheckInRequestDto(1L, "1234", signatureFile);
                Visit visit = Visit.builder()
                    .isLongTerm(false)
                    .startDate(LocalDate.now())
                    .visited(true)
                    .password("encoded_pw")
                    .build();

                given(visitRepository.findById(1L)).willReturn(Optional.of(visit));
                given(passwordEncoder.matches("1234", "encoded_pw")).willReturn(true);

                // when & then
                assertThatThrownBy(() -> visitService.checkIn(dto))
                    .isInstanceOf(CustomException.class)
                    .hasMessage("이미 체크아웃된 방문정보입니다.");
            }
        }

        @Nested
        @DisplayName("모든 입력값이 정상적이고 조건에 맞으면")
        class Context_with_valid_input {

            @Test
            @DisplayName("방문 상태를 IN_PROGRESS로 바꾸고 입실 기록을 생성한다.")
            void it_check_in_successfully() throws IOException {
                // given
                CheckInRequestDto dto = new CheckInRequestDto(1L, "1234", signatureFile);
                Visit visit = Visit.builder()
                    .id(1L)
                    .isLongTerm(false)
                    .startDate(LocalDate.now()) // 오늘 날짜
                    .visited(false)
                    .password("encoded_pw")
                    .records(new ArrayList<>())
                    .build();

                given(visitRepository.findById(1L)).willReturn(Optional.of(visit));
                given(passwordEncoder.matches("1234", "encoded_pw")).willReturn(true);
                given(s3Service.uploadFile(any())).willReturn("s3-signature-key");

                // when
                Long resultId = visitService.checkIn(dto);

                // then
                assertThat(resultId).isEqualTo(1L);
                assertThat(visit.getStatus()).isEqualTo(VisitStatus.IN_PROGRESS);
                assertThat(visit.isVisited()).isTrue();
                assertThat(visit.getRecords()).hasSize(1);
                assertThat(visit.getRecords().get(0).getSignatureKey()).isEqualTo(
                    "s3-signature-key");

                verify(s3Service, times(1)).uploadFile(any());
            }
        }
    }

    @Nested
    @DisplayName("checkOut 메서드는")
    class Describe_checkOut {

        private final Long ADMIN_ID = 1L;
        private final Long VISIT_ID = 100L;
        private final LocalDateTime CHECK_OUT_TIME = LocalDateTime.of(2026, 1, 22, 18, 0);

        @BeforeEach
        void setUpAdmin() {
            // 퇴실 처리를 하는 유저는 항상 관리 권한을 가진 것으로 가정 (공통 셋업)
            User admin = User.builder()
                .id(ADMIN_ID)
                .jobType(JobType.MANAGEMENT)
                .build();
            admin.addAuthority(Authority.ACCESS_VISIT);
            given(userRepository.findById(ADMIN_ID)).willReturn(Optional.of(admin));
        }

        @Nested
        @DisplayName("방문 상태가 IN_PROGRESS가 아니면")
        class Context_not_in_progress {

            @Test
            @DisplayName("NOT_IN_PROGRESS 예외를 던진다.")
            void it_throws_not_in_progress_exception() {
                // given: 현재 상태가 APPROVED(입실 전 대기)인 상태
                Visit visit = Visit.builder().status(VisitStatus.APPROVED).build();
                given(visitRepository.findById(VISIT_ID)).willReturn(Optional.of(visit));

                CheckOutRequestDto dto = new CheckOutRequestDto(VISIT_ID, CHECK_OUT_TIME);

                // when & then
                assertThatThrownBy(() -> visitService.checkOut(ADMIN_ID, dto))
                    .isInstanceOf(CustomException.class)
                    .hasMessage("현재 방문 상태가 '방문 중'이 아닙니다.");
            }
        }

        @Nested
        @DisplayName("퇴실 처리가 안 된 입실 기록(VisitRecord)이 없으면")
        class Context_no_active_record {

            @Test
            @DisplayName("RECORD_NOT_FOUND 예외를 던진다.")
            void it_throws_record_not_found_exception() {
                // given: 상태는 IN_PROGRESS지만, 모든 레코드에 이미 exitTime이 있는 경우
                VisitRecord completedRecord = VisitRecord.builder().exitTime(LocalDateTime.now())
                    .build();
                Visit visit = Visit.builder()
                    .status(VisitStatus.IN_PROGRESS)
                    .records(new ArrayList<>(List.of(completedRecord)))
                    .build();

                given(visitRepository.findById(VISIT_ID)).willReturn(Optional.of(visit));
                CheckOutRequestDto dto = new CheckOutRequestDto(VISIT_ID, CHECK_OUT_TIME);

                // when & then
                assertThatThrownBy(() -> visitService.checkOut(ADMIN_ID, dto))
                    .isInstanceOf(CustomException.class)
                    .hasMessage("해당 방문기록을 찾을 수 없습니다.");
            }
        }

        @Nested
        @DisplayName("장기 방문자가 정상적으로 퇴실하면")
        class Context_long_term_visit_success {

            @Test
            @DisplayName("상태가 APPROVED(승인 완료)로 돌아가고 퇴실 시간이 기록된다.")
            void it_changes_status_to_approved() {
                // given
                VisitRecord activeRecord = VisitRecord.builder().exitTime(null).build();
                Visit visit = Visit.builder()
                    .id(VISIT_ID)
                    .isLongTerm(true) // 장기 방문
                    .status(VisitStatus.IN_PROGRESS)
                    .records(new ArrayList<>(List.of(activeRecord)))
                    .build();

                given(visitRepository.findById(VISIT_ID)).willReturn(Optional.of(visit));
                CheckOutRequestDto dto = new CheckOutRequestDto(VISIT_ID, CHECK_OUT_TIME);

                // when
                visitService.checkOut(ADMIN_ID, dto);

                // then
                assertThat(visit.getStatus()).isEqualTo(VisitStatus.APPROVED); // 다시 승인 상태로
                assertThat(activeRecord.getExitTime()).isEqualTo(CHECK_OUT_TIME);
            }
        }

        @Nested
        @DisplayName("하루 방문자가 정상적으로 퇴실하면")
        class Context_one_day_visit_success {

            @Test
            @DisplayName("상태가 COMPLETED(방문 완료)로 변경되고 퇴실 시간이 기록된다.")
            void it_changes_status_to_completed() {
                // given
                VisitRecord activeRecord = VisitRecord.builder().exitTime(null).build();
                Visit visit = Visit.builder()
                    .id(VISIT_ID)
                    .isLongTerm(false) // 하루 방문
                    .status(VisitStatus.IN_PROGRESS)
                    .records(new ArrayList<>(List.of(activeRecord)))
                    .build();

                given(visitRepository.findById(VISIT_ID)).willReturn(Optional.of(visit));
                CheckOutRequestDto dto = new CheckOutRequestDto(VISIT_ID, CHECK_OUT_TIME);

                // when
                visitService.checkOut(ADMIN_ID, dto);

                // then
                assertThat(visit.getStatus()).isEqualTo(VisitStatus.COMPLETED); // 최종 완료 상태로
                assertThat(activeRecord.getExitTime()).isEqualTo(CHECK_OUT_TIME);
            }
        }
    }

    @Nested
    @DisplayName("approveVisit 메서드는")
    class Describe_approveVisit {

        @Nested
        @DisplayName("장기 방문이 아닌 신청 건을 승인하려 하면")
        class Context_with_not_long_term_visit {

            @Test
            @DisplayName("NOT_LONG_TERM_VISIT 예외를 던진다.")
            void it_throws_not_long_term_visit_exception() {
                // given: 장기 방문이 아닌(isLongTerm=false) 방문 건
                User admin = User.builder().jobType(JobType.MANAGEMENT).build();
                admin.addAuthority(Authority.ACCESS_VISIT);
                Visit oneDayVisit = Visit.builder().isLongTerm(false).status(VisitStatus.PENDING)
                    .build();

                given(userRepository.findById(any())).willReturn(Optional.of(admin));
                given(visitRepository.findById(any())).willReturn(Optional.of(oneDayVisit));

                // when & then
                assertThatThrownBy(() -> visitService.approveVisit(1L, 100L))
                    .isInstanceOf(CustomException.class)
                    .hasMessage("장기 방문 건이 아닙니다.");
            }
        }

        @Nested
        @DisplayName("정상적인 대기 상태의 장기 방문 건이면")
        class Context_with_valid_pending_visit {

            @Test
            @DisplayName("상태를 APPROVED로 변경한다.")
            void it_updates_status_to_approved() {
                // given
                User admin = User.builder().jobType(JobType.MANAGEMENT).build();
                admin.addAuthority(Authority.ACCESS_VISIT);
                Visit pendingVisit = Visit.builder().isLongTerm(true).status(VisitStatus.PENDING)
                    .build();

                given(userRepository.findById(any())).willReturn(Optional.of(admin));
                given(visitRepository.findById(any())).willReturn(Optional.of(pendingVisit));

                // when
                visitService.approveVisit(1L, 100L);

                // then
                assertThat(pendingVisit.getStatus()).isEqualTo(VisitStatus.APPROVED);
            }
        }
    }

    @Nested
    @DisplayName("getMyVisitDetail 메서드는")
    class Describe_getMyVisitDetail {

        @Nested
        @DisplayName("비밀번호가 일치하지 않으면")
        class Context_with_wrong_password {

            @Test
            @DisplayName("정보 조회를 거부하고 예외를 던진다.")
            void it_throws_exception_for_wrong_password() {
                // given
                Visit visit = Visit.builder().password("encoded_pw").build();
                MyVisitDetailRequestDto dto = new MyVisitDetailRequestDto("wrong_pw");

                given(visitRepository.findById(any())).willReturn(Optional.of(visit));
                given(passwordEncoder.matches("wrong_pw", "encoded_pw")).willReturn(false);

                // when & then
                assertThatThrownBy(() -> visitService.getMyVisitDetail(100L, dto))
                    .isInstanceOf(RuntimeException.class) // 나중에 CustomException으로 바꾸면 좋습니다!
                    .hasMessageContaining("비밀번호가 일치하지 않아");
            }
        }
    }
}


