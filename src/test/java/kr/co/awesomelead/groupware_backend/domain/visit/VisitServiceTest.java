package kr.co.awesomelead.groupware_backend.domain.visit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import kr.co.awesomelead.groupware_backend.domain.department.entity.Department;
import kr.co.awesomelead.groupware_backend.domain.department.enums.Company;
import kr.co.awesomelead.groupware_backend.domain.department.enums.DepartmentName;
import kr.co.awesomelead.groupware_backend.domain.department.repository.DepartmentRepository;
import kr.co.awesomelead.groupware_backend.domain.notification.service.NotificationService;
import kr.co.awesomelead.groupware_backend.domain.user.entity.User;
import kr.co.awesomelead.groupware_backend.domain.user.enums.Authority;
import kr.co.awesomelead.groupware_backend.domain.user.enums.JobType;
import kr.co.awesomelead.groupware_backend.domain.user.enums.Position;
import kr.co.awesomelead.groupware_backend.domain.user.repository.UserRepository;
import kr.co.awesomelead.groupware_backend.domain.visit.dto.request.CheckInRequestDto;
import kr.co.awesomelead.groupware_backend.domain.visit.dto.request.CheckOutRequestDto;
import kr.co.awesomelead.groupware_backend.domain.visit.dto.request.LongTermVisitRequestDto;
import kr.co.awesomelead.groupware_backend.domain.visit.dto.request.MyVisitUpdateRequestDto;
import kr.co.awesomelead.groupware_backend.domain.visit.dto.request.OnSiteVisitRequestDto;
import kr.co.awesomelead.groupware_backend.domain.visit.dto.request.OneDayVisitRequestDto;
import kr.co.awesomelead.groupware_backend.domain.visit.dto.request.VisitProcessRequestDto;
import kr.co.awesomelead.groupware_backend.domain.visit.dto.request.VisitSearchRequestDto;
import kr.co.awesomelead.groupware_backend.domain.visit.dto.response.MyVisitDetailResponseDto;
import kr.co.awesomelead.groupware_backend.domain.visit.dto.response.MyVisitListResponseDto;
import kr.co.awesomelead.groupware_backend.domain.visit.dto.response.VisitListResponseDto;
import kr.co.awesomelead.groupware_backend.domain.visit.entity.Visit;
import kr.co.awesomelead.groupware_backend.domain.visit.entity.VisitHost;
import kr.co.awesomelead.groupware_backend.domain.visit.entity.VisitRecord;
import kr.co.awesomelead.groupware_backend.domain.visit.enums.AdditionalPermissionType;
import kr.co.awesomelead.groupware_backend.domain.visit.enums.VisitCategory;
import kr.co.awesomelead.groupware_backend.domain.visit.enums.VisitPurpose;
import kr.co.awesomelead.groupware_backend.domain.visit.enums.VisitStatus;
import kr.co.awesomelead.groupware_backend.domain.visit.mapper.VisitMapper;
import kr.co.awesomelead.groupware_backend.domain.visit.repository.VisitRepository;
import kr.co.awesomelead.groupware_backend.domain.visit.repository.querydsl.VisitQueryRepository;
import kr.co.awesomelead.groupware_backend.domain.visit.service.VisitService;
import kr.co.awesomelead.groupware_backend.global.error.CustomException;
import kr.co.awesomelead.groupware_backend.global.error.ErrorCode;
import kr.co.awesomelead.groupware_backend.global.infra.s3.service.S3Service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@ExtendWith(MockitoExtension.class)
@ActiveProfiles("test")
public class VisitServiceTest {

    @Spy
    private VisitMapper visitMapper = org.mapstruct.factory.Mappers.getMapper(VisitMapper.class);

    @InjectMocks private VisitService visitService;

    @Mock private VisitRepository visitRepository;
    @Mock private VisitQueryRepository visitQueryRepository;
    @Mock private UserRepository userRepository;
    @Mock private DepartmentRepository departmentRepository;
    @Mock private S3Service s3Service;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private NotificationService notificationService;

    private static final Long HOST_ID = 1L;
    private static final Long VISIT_ID = 100L;
    private static final String PLAIN_PASSWORD = "1234";
    private static final String ENCODED_PASSWORD = "encoded_password";

    private Department createDepartment(Long id) {
        return Department.builder().id(id).name(DepartmentName.SALES_DEPT).build();
    }

    private User createHost() {
        User host =
                User.builder()
                        .id(HOST_ID)
                        .nameKor("담당자")
                        .workLocation(Company.AWESOME)
                        .jobType(JobType.MANAGEMENT)
                        .department(createDepartment(1L))
                        .build();
        host.addAuthority(Authority.MANAGE_VISITOR);
        return host;
    }

    private Visit createBaseVisit(VisitStatus status, VisitCategory visitCategory) {
        Visit visit =
                Visit.builder()
                        .id(VISIT_ID)
                        .password(ENCODED_PASSWORD)
                        .status(status)
                        .visitCategory(visitCategory)
                        .records(new ArrayList<>())
                        .build();
        visit.getHosts().add(VisitHost.builder().user(createHost()).build());
        return visit;
    }

    @Nested
    @DisplayName("registerOneDayPreVisit 메서드는")
    class Describe_registerOneDayPreVisit {

        @BeforeEach
        void setup() {
            // 등록 로직은 항상 호스트를 찾으므로 공통 설정
            given(userRepository.findById(any())).willReturn(Optional.of(createHost()));
        }

        @Nested
        @DisplayName("방문목적이 '시설공사'인데, '보충적허가필요여부'가 없으면")
        class Context_with_facility_construction_and_no_additional_permission_type {

            @Test
            @DisplayName("ADDITIONAL_PERMISSION_REQUIRED 예외를 던진다.")
            void it_throws_additional_permission_required_exception() {
                OneDayVisitRequestDto dto =
                        createOneDayDto(VisitPurpose.FACILITY_CONSTRUCTION, null, null);

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
                OneDayVisitRequestDto dto =
                        createOneDayDto(
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
                OneDayVisitRequestDto dto =
                        createOneDayDto(VisitPurpose.MEETING, AdditionalPermissionType.NONE, null);

                String encodedPassword = "encoded_password_1234";

                Visit mockVisit =
                        createBaseVisit(VisitStatus.NOT_VISITED, VisitCategory.PRE_ONE_DAY);
                mockVisit.setPurpose(dto.getPurpose());
                mockVisit.setVisitorName("  홍길동  ");
                mockVisit.setVisitorCompany("  테스트컴퍼니  ");
                mockVisit.setCarNumber("  12나  1234  ");

                given(passwordEncoder.encode(dto.getPassword())).willReturn(encodedPassword);
                given(visitMapper.toOneDayVisit(any(), any(), any())).willReturn(mockVisit);
                given(visitRepository.save(any(Visit.class))).willReturn(mockVisit);

                // when
                Long resultId = visitService.registerOneDayPreVisit(dto);

                // then
                assertThat(resultId).isEqualTo(VISIT_ID);
                assertThat(mockVisit.getVisitorName()).isEqualTo("홍길동");
                assertThat(mockVisit.getVisitorCompany()).isEqualTo("테스트컴퍼니");
                assertThat(mockVisit.getCarNumber()).isEqualTo("12나1234");
                verify(userRepository, times(1)).findById(1L);
                verify(passwordEncoder, times(1)).encode(dto.getPassword());
                verify(visitRepository, times(1)).save(any(Visit.class));
            }
        }

        private OneDayVisitRequestDto createOneDayDto(
                VisitPurpose purpose, AdditionalPermissionType type, String detail) {
            return OneDayVisitRequestDto.builder()
                    .visitorName("홍길동")
                    .visitorPhoneNumber("01012345678")
                    .visitorCompany("테스트컴퍼니")
                    .purpose(purpose)
                    .permissionType(type)
                    .permissionDetail(detail)
                    .visitDate(LocalDate.now().plusDays(1))
                    .plannedEntryTime(LocalTime.of(10, 0))
                    .plannedExitTime(LocalTime.of(18, 0))
                    .hostIds(List.of(1L))
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
                LongTermVisitRequestDto dto =
                        LongTermVisitRequestDto.builder()
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
                LongTermVisitRequestDto dto =
                        LongTermVisitRequestDto.builder()
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
                LongTermVisitRequestDto dto =
                        LongTermVisitRequestDto.builder()
                                .hostIds(List.of(1L))
                                .password("1234")
                                .startDate(startDate)
                                .endDate(startDate.plusMonths(3))
                                .purpose(VisitPurpose.MEETING)
                                .build();

                // Mocking (정상 저장을 위해 필요한 최소한의 세팅)
                given(userRepository.findById(any()))
                        .willReturn(Optional.of(User.builder().id(1L).build()));
                given(passwordEncoder.encode(any())).willReturn("hash");
                given(visitMapper.toLongTermVisit(any(), any(), any()))
                        .willReturn(Visit.builder().id(1L).build());
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
            OnSiteVisitRequestDto dto =
                    OnSiteVisitRequestDto.builder()
                            .visitorName("현장방문객")
                            .hostIds(List.of(1L))
                            .password("1234")
                            .signatureFile(
                                    new MockMultipartFile(
                                            "file", "sig.png", "image/png", "test".getBytes()))
                            .purpose(VisitPurpose.MEETING)
                            .permissionType(AdditionalPermissionType.NONE)
                            .build();

            User mockHost = User.builder().id(1L).build();
            Visit mockVisit =
                    Visit.builder().id(100L).visited(true).records(new ArrayList<>()).build();

            given(userRepository.findById(any())).willReturn(Optional.of(mockHost));
            given(passwordEncoder.encode(any())).willReturn("hash");
            given(s3Service.uploadFile(any())).willReturn("s3-key-123");
            given(visitMapper.toOnSiteVisit(any(), any(), any())).willReturn(mockVisit);
            given(visitRepository.save(any())).willReturn(mockVisit);

            // when
            visitService.registerOnSiteVisit(dto);

            // then
            verify(visitRepository)
                    .save(
                            argThat(
                                    visit -> {
                                        assertThat(visit.getRecords()).hasSize(1);
                                        assertThat(visit.getRecords().get(0).getSignatureKey())
                                                .isEqualTo("s3-key-123");
                                        return true;
                                    }));
        }
    }

    @Nested
    @DisplayName("getMyVisitList 메서드는")
    class Describe_getMyVisitList {

        @Nested
        @DisplayName("이름과 전화번호에 해당하는 방문 정보가 없으면")
        class Context_with_no_visit {

            @Test
            @DisplayName("VISIT_NOT_FOUND 예외를 던진다.")
            void it_throws_visit_not_found() {
                // given
                VisitSearchRequestDto dto = new VisitSearchRequestDto("홍길동", "01012345678", "1234");
                given(
                                visitRepository.findByVisitorNameAndPhoneNumberHashOrderByIdDesc(
                                        dto.getName(), Visit.hashValue(dto.getPhoneNumber())))
                        .willReturn(List.of());

                // when & then
                assertThatThrownBy(() -> visitService.getMyVisitList(dto))
                        .isInstanceOf(CustomException.class)
                        .hasMessage(ErrorCode.VISIT_NOT_FOUND.getMessage());
            }
        }

        @Nested
        @DisplayName("방문 정보는 있지만 비밀번호가 일치하지 않으면")
        class Context_with_invalid_password {

            @Test
            @DisplayName("VISITOR_AUTHENTICATION_FAILED 예외를 던진다.")
            void it_throws_visitor_authentication_failed() {
                // given
                VisitSearchRequestDto dto = new VisitSearchRequestDto("홍길동", "01012345678", "1234");
                Visit visit = createBaseVisit(VisitStatus.NOT_VISITED, VisitCategory.PRE_ONE_DAY);
                visit.setPurpose(VisitPurpose.MEETING);
                given(
                                visitRepository.findByVisitorNameAndPhoneNumberHashOrderByIdDesc(
                                        dto.getName(), Visit.hashValue(dto.getPhoneNumber())))
                        .willReturn(List.of(visit));
                given(passwordEncoder.matches(dto.getPassword(), ENCODED_PASSWORD))
                        .willReturn(false);

                // when & then
                assertThatThrownBy(() -> visitService.getMyVisitList(dto))
                        .isInstanceOf(CustomException.class)
                        .hasMessage(ErrorCode.VISITOR_AUTHENTICATION_FAILED.getMessage());
            }
        }

        @Nested
        @DisplayName("방문 정보와 비밀번호가 모두 일치하면")
        class Context_with_valid_credentials {

            @Test
            @DisplayName("내 방문 목록을 반환한다.")
            void it_returns_my_visit_list() {
                // given
                VisitSearchRequestDto dto = new VisitSearchRequestDto("홍길동", "01012345678", "1234");
                Visit visit = createBaseVisit(VisitStatus.NOT_VISITED, VisitCategory.PRE_ONE_DAY);
                visit.setPurpose(VisitPurpose.MEETING);
                given(
                                visitRepository.findByVisitorNameAndPhoneNumberHashOrderByIdDesc(
                                        dto.getName(), Visit.hashValue(dto.getPhoneNumber())))
                        .willReturn(List.of(visit));
                given(passwordEncoder.matches(dto.getPassword(), ENCODED_PASSWORD))
                        .willReturn(true);

                // when
                List<MyVisitListResponseDto> result = visitService.getMyVisitList(dto);

                // then
                assertThat(result).hasSize(1);
                assertThat(result.get(0).getPurpose()).isEqualTo(VisitPurpose.MEETING);
                assertThat(result.get(0).getVisitCategory()).isEqualTo(VisitCategory.PRE_ONE_DAY);
            }
        }
    }

    @Nested
    @DisplayName("getMyVisitDetail 메서드는")
    class Describe_getMyVisitDetail {

        @Test
        @DisplayName("내방객 본인 상세 조회에서는 퇴실 시간 등록/수정자 정보를 노출하지 않는다.")
        void it_hides_exit_time_updated_by() {
            // given
            User manager = User.builder().id(1L).nameKor("김관리").position(Position.MANAGER).build();
            manager.setDepartment(createDepartment(1L));

            VisitRecord record =
                    VisitRecord.builder()
                            .id(10L)
                            .entryTime(LocalDateTime.of(2026, 1, 22, 10, 0))
                            .exitTime(LocalDateTime.of(2026, 1, 22, 18, 0))
                            .exitTimeUpdatedBy(manager)
                            .build();

            Visit visit = createBaseVisit(VisitStatus.COMPLETED, VisitCategory.PRE_ONE_DAY);
            visit.getRecords().add(record);
            given(visitRepository.findById(VISIT_ID)).willReturn(Optional.of(visit));

            // when
            MyVisitDetailResponseDto result = visitService.getMyVisitDetail(VISIT_ID);

            // then
            assertThat(result.getRecords()).hasSize(1);
            assertThat(result.getRecords().get(0).getExitTimeUpdatedBy()).isNull();
        }
    }

    @Nested
    @DisplayName("updateMyVisit 메서드는")
    class Describe_updateMyVisit {

        @Nested
        @DisplayName("존재하지 않는 방문 ID가 주어지면")
        class Context_with_non_existent_id {

            @Test
            @DisplayName("VISIT_NOT_FOUND 예외를 던진다.")
            void it_throws_visit_not_found() {
                // given
                MyVisitUpdateRequestDto dto =
                        MyVisitUpdateRequestDto.builder().password(PLAIN_PASSWORD).build();
                given(visitRepository.findById(VISIT_ID)).willReturn(Optional.empty());

                // when & then
                assertThatThrownBy(() -> visitService.updateMyVisit(VISIT_ID, dto))
                        .isInstanceOf(CustomException.class)
                        .hasMessage("해당 방문정보를 찾을 수 없습니다.");
            }
        }

        @Nested
        @DisplayName("비밀번호가 일치하지 않으면")
        class Context_with_invalid_password {

            @Test
            @DisplayName("INVALID_PASSWORD 예외를 던진다.")
            void it_throws_invalid_password() {
                // given
                MyVisitUpdateRequestDto dto =
                        MyVisitUpdateRequestDto.builder().password(PLAIN_PASSWORD).build();
                Visit visit = createBaseVisit(VisitStatus.NOT_VISITED, VisitCategory.PRE_ONE_DAY);

                given(visitRepository.findById(VISIT_ID)).willReturn(Optional.of(visit));
                given(passwordEncoder.matches(PLAIN_PASSWORD, ENCODED_PASSWORD)).willReturn(false);

                // when & then
                assertThatThrownBy(() -> visitService.updateMyVisit(VISIT_ID, dto))
                        .isInstanceOf(CustomException.class)
                        .hasMessage("유효하지 않은 비밀번호입니다.");
            }
        }

        @Nested
        @DisplayName("수정 불가능한 상태(COMPLETED)인 경우")
        class Context_with_completed_status {

            @Test
            @DisplayName("INVALID_VISIT_STATUS 예외를 던진다.")
            void it_throws_invalid_status() {
                // given
                MyVisitUpdateRequestDto dto =
                        MyVisitUpdateRequestDto.builder().password(PLAIN_PASSWORD).build();
                Visit visit = createBaseVisit(VisitStatus.COMPLETED, VisitCategory.PRE_ONE_DAY);

                given(visitRepository.findById(VISIT_ID)).willReturn(Optional.of(visit));
                given(passwordEncoder.matches(PLAIN_PASSWORD, ENCODED_PASSWORD)).willReturn(true);

                // when & then
                assertThatThrownBy(() -> visitService.updateMyVisit(VISIT_ID, dto))
                        .isInstanceOf(CustomException.class)
                        .hasMessage("승인 가능한 상태가 아닙니다.");
            }
        }

        @Nested
        @DisplayName("정상적인 하루 방문 수정 요청이 오면")
        class Context_with_valid_one_day_request {

            @Test
            @DisplayName("정보를 업데이트하고 날짜를 동기화한다.")
            void it_updates_one_day_visit() {
                // given
                LocalDate newDate = LocalDate.now().plusDays(5);
                MyVisitUpdateRequestDto dto =
                        MyVisitUpdateRequestDto.builder()
                                .password(PLAIN_PASSWORD)
                                .startDate(newDate)
                                .visitorName("  수정된이름  ")
                                .visitorCompany("  수정된회사  ")
                                .carNumber("  34나  5678  ")
                                .build();

                Visit visit = createBaseVisit(VisitStatus.NOT_VISITED, VisitCategory.PRE_ONE_DAY);
                visit.setStartDate(newDate);

                given(visitRepository.findById(VISIT_ID)).willReturn(Optional.of(visit));
                given(passwordEncoder.matches(PLAIN_PASSWORD, ENCODED_PASSWORD)).willReturn(true);

                // when
                visitService.updateMyVisit(VISIT_ID, dto);

                // then
                assertThat(visit.getVisitorName()).isEqualTo("수정된이름");
                assertThat(visit.getVisitorCompany()).isEqualTo("수정된회사");
                assertThat(visit.getCarNumber()).isEqualTo("34나5678");
                assertThat(visit.getEndDate()).isEqualTo(visit.getStartDate());
                assertThat(visit.getStartDate()).isEqualTo(newDate);
            }
        }

        @Nested
        @DisplayName("이미 승인된 장기 방문 건을 수정할 때")
        class Context_with_approved_long_term_update {

            @Test
            @DisplayName("방문 기록이 없으면(visited=false) 정보를 업데이트하고 상태를 PENDING으로 변경한다.")
            void it_updates_and_resets_status_when_no_records() {
                // given
                MyVisitUpdateRequestDto dto =
                        MyVisitUpdateRequestDto.builder()
                                .password(PLAIN_PASSWORD)
                                .visitorName("이름수정")
                                .build();

                Visit visit = createBaseVisit(VisitStatus.APPROVED, VisitCategory.PRE_LONG_TERM);
                visit.setStartDate(LocalDate.now());
                visit.setEndDate(LocalDate.now().plusMonths(1));
                visit.setVisited(false);

                given(visitRepository.findById(VISIT_ID)).willReturn(Optional.of(visit));
                given(passwordEncoder.matches(PLAIN_PASSWORD, ENCODED_PASSWORD)).willReturn(true);

                // when
                visitService.updateMyVisit(VISIT_ID, dto);

                // then
                assertThat(visit.getVisitorName()).isEqualTo("이름수정");
                assertThat(visit.getStatus()).isEqualTo(VisitStatus.PENDING); // 다시 대기 상태로!
            }

            @Test
            @DisplayName("이미 방문 기록이 존재하면(visited=true) INVALID_VISIT_STATUS 예외를 던진다.")
            void it_throws_exception_when_already_visited() {
                // given
                MyVisitUpdateRequestDto dto =
                        MyVisitUpdateRequestDto.builder().password(PLAIN_PASSWORD).build();

                Visit visit = createBaseVisit(VisitStatus.APPROVED, VisitCategory.PRE_LONG_TERM);
                visit.setVisited(true); // ★ 핵심: 이미 한 번이라도 입실했던 상태

                given(visitRepository.findById(VISIT_ID)).willReturn(Optional.of(visit));
                given(passwordEncoder.matches(PLAIN_PASSWORD, ENCODED_PASSWORD)).willReturn(true);

                // when & then
                assertThatThrownBy(() -> visitService.updateMyVisit(VISIT_ID, dto))
                        .isInstanceOf(CustomException.class)
                        .hasMessage("승인 가능한 상태가 아닙니다."); // ErrorCode.INVALID_VISIT_STATUS
            }
        }

        @Nested
        @DisplayName("hostId가 주어지면")
        class Context_with_host_id {

            @Test
            @DisplayName("담당자가 새 User로 변경된다.")
            void it_updates_host() {
                // given
                Long newHostId = 2L;
                MyVisitUpdateRequestDto dto =
                        MyVisitUpdateRequestDto.builder()
                                .password(PLAIN_PASSWORD)
                                .hostIds(List.of(newHostId))
                                .build();

                Visit visit = createBaseVisit(VisitStatus.NOT_VISITED, VisitCategory.PRE_ONE_DAY);
                User newHost = User.builder().id(newHostId).build();

                given(visitRepository.findById(VISIT_ID)).willReturn(Optional.of(visit));
                given(passwordEncoder.matches(PLAIN_PASSWORD, ENCODED_PASSWORD)).willReturn(true);
                given(userRepository.findById(newHostId)).willReturn(Optional.of(newHost));

                // when
                visitService.updateMyVisit(VISIT_ID, dto);

                // then
                assertThat(visit.getHosts()).hasSize(1);
                assertThat(visit.getHosts().get(0).getUser().getId()).isEqualTo(newHostId);
            }
        }

        @Nested
        @DisplayName("visitorPhoneNumber가 주어지면")
        class Context_with_visitor_phone_number {

            @Test
            @DisplayName("전화번호와 phoneNumberHash가 함께 업데이트된다.")
            void it_updates_phone_and_hash() {
                // given
                String newPhone = "01099998888";
                MyVisitUpdateRequestDto dto =
                        MyVisitUpdateRequestDto.builder()
                                .password(PLAIN_PASSWORD)
                                .visitorPhoneNumber(newPhone)
                                .build();

                Visit visit = createBaseVisit(VisitStatus.NOT_VISITED, VisitCategory.PRE_ONE_DAY);
                visit.setPhoneNumberHash("old-hash");

                given(visitRepository.findById(VISIT_ID)).willReturn(Optional.of(visit));
                given(passwordEncoder.matches(PLAIN_PASSWORD, ENCODED_PASSWORD)).willReturn(true);

                // when
                visitService.updateMyVisit(VISIT_ID, dto);

                // then
                assertThat(visit.getVisitorPhoneNumber()).isEqualTo(newPhone);
                assertThat(visit.getPhoneNumberHash()).isNotNull();
                assertThat(visit.getPhoneNumberHash()).isEqualTo(Visit.hashValue(newPhone));
            }
        }
    }

    @Nested
    @DisplayName("checkIn 메서드는")
    class Describe_checkIn {

        @Mock private MockMultipartFile signatureFile;

        @Nested
        @DisplayName("하루 방문인데 방문 예정일이 오늘이 아니면")
        class Context_with_invalid_visit_date {

            @Test
            @DisplayName("NOT_VISIT_DATE 예외를 던진다.")
            void it_throws_not_visit_date_exception() throws IOException {
                // given
                CheckInRequestDto dto = new CheckInRequestDto(1L, signatureFile);
                // 어제 날짜로 예약된 하루 방문 건
                Visit visit = createBaseVisit(VisitStatus.NOT_VISITED, VisitCategory.PRE_ONE_DAY);
                visit.setStartDate(LocalDate.now().minusDays(1));

                given(visitRepository.findById(1L)).willReturn(Optional.of(visit));

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
                CheckInRequestDto dto = new CheckInRequestDto(1L, signatureFile);
                Visit visit = createBaseVisit(VisitStatus.COMPLETED, VisitCategory.PRE_ONE_DAY);
                visit.setStartDate(LocalDate.now());
                visit.setVisited(true);

                given(visitRepository.findById(1L)).willReturn(Optional.of(visit));

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
                CheckInRequestDto dto = new CheckInRequestDto(1L, signatureFile);
                Visit visit = createBaseVisit(VisitStatus.NOT_VISITED, VisitCategory.PRE_ONE_DAY);
                visit.setStartDate(LocalDate.now());

                given(visitRepository.findById(1L)).willReturn(Optional.of(visit));
                given(s3Service.uploadFile(any())).willReturn("s3-signature-key");

                // when
                Long resultId = visitService.checkIn(dto);

                // then
                assertThat(resultId).isEqualTo(VISIT_ID);
                assertThat(visit.getStatus()).isEqualTo(VisitStatus.IN_PROGRESS);
                assertThat(visit.isVisited()).isTrue();
                assertThat(visit.getRecords()).hasSize(1);
                assertThat(visit.getRecords().get(0).getSignatureKey())
                        .isEqualTo("s3-signature-key");

                verify(s3Service, times(1)).uploadFile(any());
            }
        }

        @Nested
        @DisplayName("장기 방문자가 한 번 입퇴실한 후 다시 입실을 시도하면")
        class Context_long_term_reentry {

            @Test
            @DisplayName("정상적으로 재입실할 수 있어야 한다")
            void it_allows_reentry_for_long_term_visit() throws IOException {
                // given
                Long visitId = 200L;
                Long adminId = 2L;
                MockMultipartFile sig =
                        new MockMultipartFile(
                                "signature", "sig.png", "image/png", "test".getBytes());
                CheckInRequestDto checkInDto = new CheckInRequestDto(visitId, sig);

                Visit visit =
                        Visit.builder()
                                .id(visitId)
                                .status(VisitStatus.APPROVED)
                                .visitCategory(VisitCategory.PRE_LONG_TERM)
                                .startDate(LocalDate.now().minusDays(5))
                                .endDate(LocalDate.now().plusDays(5))
                                .records(new ArrayList<>())
                                .visited(false)
                                .build();
                visit.getHosts().add(VisitHost.builder().user(createHost()).build());

                User admin =
                        User.builder()
                                .id(adminId)
                                .jobType(JobType.MANAGEMENT)
                                .department(createDepartment(1L))
                                .build();
                admin.addAuthority(Authority.MANAGE_VISITOR);

                given(visitRepository.findById(visitId)).willReturn(Optional.of(visit));
                given(s3Service.uploadFile(any())).willReturn("s3-key");

                // 1. 첫 번째 입실
                visitService.checkIn(checkInDto);
                assertThat(visit.getStatus()).isEqualTo(VisitStatus.IN_PROGRESS);
                assertThat(visit.getRecords()).hasSize(1);
                Long recordId = 10L;
                visit.getRecords().get(0).setId(recordId);

                // 2. 퇴실 처리
                given(userRepository.findById(adminId)).willReturn(Optional.of(admin));
                CheckOutRequestDto checkOutDto =
                        new CheckOutRequestDto(visitId, recordId, LocalDateTime.now());
                visitService.checkOut(adminId, checkOutDto);
                assertThat(visit.getStatus()).isEqualTo(VisitStatus.APPROVED);

                // 3. 두 번째 입실 — 버그 수정 후 성공해야 한다
                assertDoesNotThrow(() -> visitService.checkIn(checkInDto));
                assertThat(visit.getStatus()).isEqualTo(VisitStatus.IN_PROGRESS);
                assertThat(visit.getRecords()).hasSize(2);
            }
        }

        @Nested
        @DisplayName("장기 방문자가 이미 입실 중(IN_PROGRESS)일 때 재입실 시도하면")
        class Context_long_term_already_in_progress {

            @Test
            @DisplayName("ALREADY_CHECKED_IN 예외를 던진다")
            void it_throws_already_checked_in() throws IOException {
                // given
                Long visitId = 201L;
                MockMultipartFile sig =
                        new MockMultipartFile(
                                "signature", "sig.png", "image/png", "test".getBytes());
                CheckInRequestDto dto = new CheckInRequestDto(visitId, sig);

                Visit visit =
                        Visit.builder()
                                .id(visitId)
                                .status(VisitStatus.IN_PROGRESS)
                                .visitCategory(VisitCategory.PRE_LONG_TERM)
                                .startDate(LocalDate.now().minusDays(5))
                                .endDate(LocalDate.now().plusDays(5))
                                .records(new ArrayList<>())
                                .visited(true)
                                .build();

                given(visitRepository.findById(visitId)).willReturn(Optional.of(visit));

                // when / then — ALREADY_CHECKED_IN은 아직 ErrorCode에 없으므로 컴파일 에러 (Red)
                assertThatThrownBy(() -> visitService.checkIn(dto))
                        .isInstanceOf(CustomException.class)
                        .hasMessageContaining(ErrorCode.ALREADY_CHECKED_IN.getMessage());
            }
        }

        @Nested
        @DisplayName("장기 방문 기간 외 날짜에 입실을 시도하면")
        class Context_long_term_outside_date_range {

            @Test
            @DisplayName("NOT_VISIT_DATE 예외를 던진다")
            void it_throws_not_visit_date_for_long_term() throws IOException {
                // given
                Long visitId = 202L;
                MockMultipartFile sig =
                        new MockMultipartFile(
                                "signature", "sig.png", "image/png", "test".getBytes());
                CheckInRequestDto dto = new CheckInRequestDto(visitId, sig);

                Visit visit =
                        Visit.builder()
                                .id(visitId)
                                .status(VisitStatus.APPROVED)
                                .visitCategory(VisitCategory.PRE_LONG_TERM)
                                .startDate(LocalDate.now().plusDays(10))
                                .endDate(LocalDate.now().plusDays(20))
                                .records(new ArrayList<>())
                                .visited(false)
                                .build();

                given(visitRepository.findById(visitId)).willReturn(Optional.of(visit));

                // when / then
                assertThatThrownBy(() -> visitService.checkIn(dto))
                        .isInstanceOf(CustomException.class)
                        .hasMessageContaining(ErrorCode.NOT_VISIT_DATE.getMessage());
            }
        }
    }

    @Nested
    @DisplayName("checkOut 메서드는")
    class Describe_checkOut {

        private final Long ADMIN_ID = 1L;
        private final Long VISIT_ID = 100L;
        private final Long RECORD_ID = 10L;
        private final LocalDateTime ENTRY_TIME = LocalDateTime.of(2026, 1, 22, 10, 0);
        private final LocalDateTime CHECK_OUT_TIME = LocalDateTime.of(2026, 1, 22, 18, 0);

        @BeforeEach
        void setUpAdmin() {
            User admin =
                    User.builder()
                            .id(ADMIN_ID)
                            .nameKor("김관리")
                            .jobType(JobType.MANAGEMENT)
                            .position(Position.MANAGER)
                            .build();
            admin.setDepartment(createDepartment(1L));
            admin.addAuthority(Authority.MANAGE_VISITOR);
            given(userRepository.findById(ADMIN_ID)).willReturn(Optional.of(admin));
        }

        @Nested
        @DisplayName("최초 퇴실 처리인 경우 (exitTime이 null)")
        class Context_initial_checkout {

            @Test
            @DisplayName("방문 상태가 IN_PROGRESS가 아니면 NOT_IN_PROGRESS 예외를 던진다.")
            void it_throws_not_in_progress_exception() {
                // given: 상태가 APPROVED(입실 전)인데 퇴실을 시도하는 경우
                VisitRecord record = VisitRecord.builder().id(RECORD_ID).exitTime(null).build();
                Visit visit = createBaseVisit(VisitStatus.APPROVED, VisitCategory.PRE_ONE_DAY);
                visit.getRecords().add(record);

                given(visitRepository.findById(VISIT_ID)).willReturn(Optional.of(visit));
                CheckOutRequestDto dto =
                        new CheckOutRequestDto(VISIT_ID, RECORD_ID, CHECK_OUT_TIME);

                // when & then
                assertThatThrownBy(() -> visitService.checkOut(ADMIN_ID, dto))
                        .isInstanceOf(CustomException.class)
                        .hasMessage("현재 방문 상태가 '방문 중'이 아닙니다.");
            }

            @Test
            @DisplayName("하루 방문자라면 상태를 COMPLETED로 변경하고 시간을 기록한다.")
            void it_changes_status_to_completed_for_one_day_visit() {
                // given
                VisitRecord record =
                        VisitRecord.builder()
                                .id(RECORD_ID)
                                .entryTime(ENTRY_TIME)
                                .exitTime(null)
                                .build();

                Visit visit = createBaseVisit(VisitStatus.IN_PROGRESS, VisitCategory.PRE_ONE_DAY);
                visit.getRecords().add(record);

                given(visitRepository.findById(VISIT_ID)).willReturn(Optional.of(visit));
                CheckOutRequestDto dto =
                        new CheckOutRequestDto(VISIT_ID, RECORD_ID, CHECK_OUT_TIME);

                // when
                visitService.checkOut(ADMIN_ID, dto);

                // then
                assertThat(visit.getStatus()).isEqualTo(VisitStatus.COMPLETED);
                assertThat(record.getExitTime()).isEqualTo(CHECK_OUT_TIME);
                assertThat(record.getExitTimeUpdatedBy().getId()).isEqualTo(ADMIN_ID);
            }

            @Test
            @DisplayName("장기 방문자라면 상태를 APPROVED로 변경하고 시간을 기록한다.")
            void it_changes_status_to_approved_for_long_term_visit() {
                // given
                VisitRecord record =
                        VisitRecord.builder()
                                .id(RECORD_ID)
                                .entryTime(ENTRY_TIME)
                                .exitTime(null)
                                .build();
                Visit visit = createBaseVisit(VisitStatus.IN_PROGRESS, VisitCategory.PRE_LONG_TERM);
                visit.getRecords().add(record);

                given(visitRepository.findById(VISIT_ID)).willReturn(Optional.of(visit));
                CheckOutRequestDto dto =
                        new CheckOutRequestDto(VISIT_ID, RECORD_ID, CHECK_OUT_TIME);

                // when
                visitService.checkOut(ADMIN_ID, dto);

                // then
                assertThat(visit.getStatus()).isEqualTo(VisitStatus.APPROVED);
                assertThat(record.getExitTime()).isEqualTo(CHECK_OUT_TIME);
            }
        }

        @Nested
        @DisplayName("퇴실 시간 수정인 경우 (exitTime이 이미 존재)")
        class Context_time_correction {

            @Test
            @DisplayName("방문 상태를 변경하지 않고 시간만 업데이트한다.")
            void it_updates_only_time_without_changing_status() {
                // given: 이미 COMPLETED 상태이고 퇴실 시간도 있는 경우
                LocalDateTime oldExitTime = LocalDateTime.of(2026, 1, 22, 17, 0);
                LocalDateTime newExitTime = LocalDateTime.of(2026, 1, 22, 19, 0);

                VisitRecord record =
                        VisitRecord.builder()
                                .id(RECORD_ID)
                                .entryTime(ENTRY_TIME)
                                .exitTime(oldExitTime)
                                .build();
                Visit visit = createBaseVisit(VisitStatus.COMPLETED, VisitCategory.PRE_ONE_DAY);
                visit.getRecords().add(record);

                given(visitRepository.findById(VISIT_ID)).willReturn(Optional.of(visit));
                CheckOutRequestDto dto = new CheckOutRequestDto(VISIT_ID, RECORD_ID, newExitTime);

                // when
                visitService.checkOut(ADMIN_ID, dto);

                // then
                assertThat(visit.getStatus()).isEqualTo(VisitStatus.COMPLETED); // 상태 유지
                assertThat(record.getExitTime()).isEqualTo(newExitTime); // 시간만 업데이트
                assertThat(record.getExitTimeUpdatedBy().getId()).isEqualTo(ADMIN_ID);
            }
        }

        @Nested
        @DisplayName("검증 로직 작동 확인")
        class Context_validations {

            @Test
            @DisplayName("퇴실 시간이 입실 시간보다 빠르면 INVALID_CHECKOUT_TIME 예외를 던진다.")
            void it_throws_invalid_checkout_time() {
                // given: 입실은 10시인데 퇴실을 09시로 요청한 경우
                LocalDateTime invalidTime = ENTRY_TIME.minusHours(1);
                VisitRecord record =
                        VisitRecord.builder()
                                .id(RECORD_ID)
                                .entryTime(ENTRY_TIME)
                                .exitTime(null)
                                .build();
                Visit visit = createBaseVisit(VisitStatus.IN_PROGRESS, VisitCategory.PRE_ONE_DAY);
                visit.getRecords().add(record);

                given(visitRepository.findById(VISIT_ID)).willReturn(Optional.of(visit));
                CheckOutRequestDto dto = new CheckOutRequestDto(VISIT_ID, RECORD_ID, invalidTime);

                // when & then
                assertThatThrownBy(() -> visitService.checkOut(ADMIN_ID, dto))
                        .isInstanceOf(CustomException.class)
                        .hasMessage("퇴실 시간은 입실 시간보다 빠를 수 없습니다.");
            }
        }
    }

    @Nested
    @DisplayName("processVisit 메서드는")
    class Describe_processVisit {

        @Nested
        @DisplayName("장기 방문이 아닌 신청 건을 처리하려 하면")
        class Context_with_not_long_term_visit {

            @Test
            @DisplayName("NOT_LONG_TERM_VISIT 예외를 던진다.")
            void it_throws_not_long_term_visit_exception() {
                // given
                User admin = createHost();
                Visit oneDayVisit = createBaseVisit(VisitStatus.PENDING, VisitCategory.PRE_ONE_DAY);
                VisitProcessRequestDto dto = new VisitProcessRequestDto(VisitStatus.APPROVED, null);

                given(userRepository.findById(any())).willReturn(Optional.of(admin));
                given(visitRepository.findById(any())).willReturn(Optional.of(oneDayVisit));

                // when & then
                assertThatThrownBy(() -> visitService.processVisit(1L, 100L, dto))
                        .isInstanceOf(CustomException.class)
                        .hasMessage("장기 방문 건이 아닙니다.");
            }
        }

        @Nested
        @DisplayName("승인(APPROVED) 요청을 받으면")
        class Context_with_approve_request {

            @Test
            @DisplayName("방문 상태를 APPROVED로 변경한다.")
            void it_updates_status_to_approved() {
                // given
                User admin = createHost();
                Visit pendingVisit =
                        createBaseVisit(VisitStatus.PENDING, VisitCategory.PRE_LONG_TERM);
                VisitProcessRequestDto dto = new VisitProcessRequestDto(VisitStatus.APPROVED, null);

                given(userRepository.findById(any())).willReturn(Optional.of(admin));
                given(visitRepository.findById(any())).willReturn(Optional.of(pendingVisit));

                // when
                visitService.processVisit(1L, 100L, dto);

                // then
                assertThat(pendingVisit.getStatus()).isEqualTo(VisitStatus.APPROVED);
                assertThat(pendingVisit.getRejectionReason()).isNull();
            }
        }

        @Nested
        @DisplayName("반려(REJECTED) 요청을 사유와 함께 받으면")
        class Context_with_reject_request {

            @Test
            @DisplayName("방문 상태를 REJECTED로 변경하고 사유를 기록한다.")
            void it_updates_status_to_rejected_and_records_reason() {
                // given
                User admin = createHost();
                Visit pendingVisit =
                        createBaseVisit(VisitStatus.PENDING, VisitCategory.PRE_LONG_TERM);
                String reason = "방문 목적 부적합";
                VisitProcessRequestDto dto =
                        new VisitProcessRequestDto(VisitStatus.REJECTED, reason);

                given(userRepository.findById(any())).willReturn(Optional.of(admin));
                given(visitRepository.findById(any())).willReturn(Optional.of(pendingVisit));

                // when
                visitService.processVisit(1L, 100L, dto);

                // then
                assertThat(pendingVisit.getStatus()).isEqualTo(VisitStatus.REJECTED);
                assertThat(pendingVisit.getRejectionReason()).isEqualTo(reason);
            }
        }

        @Nested
        @DisplayName("이미 처리된(PENDING이 아닌) 건을 처리하려 하면")
        class Context_with_already_processed_visit {

            @Test
            @DisplayName("INVALID_VISIT_STATUS 예외를 던진다.")
            void it_throws_invalid_visit_status_exception() {
                // given
                User admin = createHost();
                Visit approvedVisit =
                        createBaseVisit(VisitStatus.APPROVED, VisitCategory.PRE_LONG_TERM);
                VisitProcessRequestDto dto =
                        new VisitProcessRequestDto(VisitStatus.REJECTED, "뒤늦은 반려");

                given(userRepository.findById(any())).willReturn(Optional.of(admin));
                given(visitRepository.findById(any())).willReturn(Optional.of(approvedVisit));

                // when & then
                assertThatThrownBy(() -> visitService.processVisit(1L, 100L, dto))
                        .isInstanceOf(CustomException.class)
                        .hasMessage("승인 가능한 상태가 아닙니다.");
            }
        }
    }

    @Nested
    @DisplayName("getVisitsForAdmin 메서드는")
    class Describe_getVisitsForAdmin {

        private final Long ADMIN_ID = 1L;
        private final Pageable pageable = PageRequest.of(0, 20);

        @BeforeEach
        void setUpAdmin() {
            User admin = User.builder().id(ADMIN_ID).jobType(JobType.MANAGEMENT).build();
            admin.addAuthority(Authority.MANAGE_VISITOR);
            given(userRepository.findById(ADMIN_ID)).willReturn(Optional.of(admin));
        }

        @Test
        @DisplayName("날짜 범위 파라미터를 포함하여 저장소를 호출한다.")
        void it_calls_repository_with_date_range() {
            // given
            LocalDate startDate = LocalDate.of(2024, 7, 1);
            LocalDate endDate = LocalDate.of(2024, 7, 5);
            given(visitQueryRepository.findVisitsForAdmin(null, null, startDate, endDate, pageable))
                    .willReturn(new PageImpl<>(List.of()));

            // when
            Page<VisitListResponseDto> result =
                    visitService.getVisitsForAdmin(
                            ADMIN_ID, null, null, startDate, endDate, pageable);

            // then
            verify(visitQueryRepository)
                    .findVisitsForAdmin(null, null, startDate, endDate, pageable);
            assertThat(result.getContent()).isEmpty();
        }

        @Test
        @DisplayName("날짜 범위 없이 호출하면 null로 저장소를 호출한다.")
        void it_calls_repository_with_null_dates_when_not_provided() {
            // given
            given(visitQueryRepository.findVisitsForAdmin(null, null, null, null, pageable))
                    .willReturn(new PageImpl<>(List.of()));

            // when
            visitService.getVisitsForAdmin(ADMIN_ID, null, null, null, null, pageable);

            // then
            verify(visitQueryRepository).findVisitsForAdmin(null, null, null, null, pageable);
        }
    }

    @Nested
    @DisplayName("VisitHost 엔티티는")
    class Describe_VisitHost {

        @Nested
        @DisplayName("visit과 user를 인자로 생성하면")
        class Context_with_visit_and_user {

            @Test
            @DisplayName("visit 필드와 user 필드가 올바르게 초기화된다.")
            void visitHost_create_success() {
                // given
                // TODO: 구현 필요 — VisitHost 클래스가 아직 없으므로 컴파일 에러 발생
                Visit visit = createBaseVisit(VisitStatus.PENDING, VisitCategory.PRE_LONG_TERM);
                User user = createHost();

                // when
                VisitHost visitHost = VisitHost.builder().visit(visit).user(user).build();

                // then
                assertThat(visitHost.getVisit()).isEqualTo(visit);
                assertThat(visitHost.getUser()).isEqualTo(user);
            }
        }

        @Nested
        @DisplayName("visit 없이 생성하면")
        class Context_without_visit {

            @Test
            @DisplayName("visit 필드가 null이다.")
            void visitHost_visit_is_null_when_not_set() {
                // given
                // TODO: 구현 필요 — VisitHost 클래스가 아직 없으므로 컴파일 에러 발생
                User user = createHost();

                // when
                VisitHost visitHost = VisitHost.builder().user(user).build();

                // then
                assertThat(visitHost.getVisit()).isNull();
            }
        }

        @Nested
        @DisplayName("user 없이 생성하면")
        class Context_without_user {

            @Test
            @DisplayName("user 필드가 null이다.")
            void visitHost_user_is_null_when_not_set() {
                // given
                // TODO: 구현 필요 — VisitHost 클래스가 아직 없으므로 컴파일 에러 발생
                Visit visit = createBaseVisit(VisitStatus.PENDING, VisitCategory.PRE_LONG_TERM);

                // when
                VisitHost visitHost = VisitHost.builder().visit(visit).build();

                // then
                assertThat(visitHost.getUser()).isNull();
            }
        }
    }

    @Nested
    @DisplayName("Visit 엔티티의 hosts 필드는")
    class Describe_VisitHosts_Field {

        @Nested
        @DisplayName("빌더로 hosts 리스트를 포함하여 생성하면")
        class Context_with_hosts {

            @Test
            @DisplayName("getHosts()가 해당 VisitHost 목록을 반환한다.")
            void getHosts_returnsProvidedHosts() {
                // given
                Visit visit = createBaseVisit(VisitStatus.PENDING, VisitCategory.PRE_LONG_TERM);
                User user = createHost();
                VisitHost visitHost = VisitHost.builder().visit(visit).user(user).build();
                List<VisitHost> hosts = List.of(visitHost);

                // when
                // TODO: 구현 필요 — Visit.hosts 필드가 없으므로 컴파일 에러 발생
                Visit visitWithHosts =
                        Visit.builder()
                                .id(VISIT_ID)
                                .password(ENCODED_PASSWORD)
                                .status(VisitStatus.PENDING)
                                .visitCategory(VisitCategory.PRE_LONG_TERM)
                                .hosts(hosts)
                                .build();

                // then
                assertThat(visitWithHosts.getHosts()).containsExactlyElementsOf(hosts);
            }
        }

        @Nested
        @DisplayName("기본 빌더로 생성하면")
        class Context_with_default_builder {

            @Test
            @DisplayName("getHosts()가 빈 리스트를 반환한다.")
            void getHosts_returnsEmptyListByDefault() {
                // given & when
                // TODO: 구현 필요 — Visit.hosts 필드가 없으므로 컴파일 에러 발생
                Visit visit =
                        Visit.builder()
                                .id(VISIT_ID)
                                .password(ENCODED_PASSWORD)
                                .status(VisitStatus.PENDING)
                                .visitCategory(VisitCategory.PRE_LONG_TERM)
                                .build();

                // then
                assertThat(visit.getHosts()).isNotNull();
                assertThat(visit.getHosts()).isEmpty();
            }
        }
    }

    @Nested
    @DisplayName("getVisitDetailForAdmin 메서드는")
    class Describe_getVisitDetailForAdmin {

        private final Long ADMIN_ID = 1L;

        @BeforeEach
        void setUpAdmin() {
            User admin = User.builder().id(ADMIN_ID).jobType(JobType.MANAGEMENT).build();
            admin.addAuthority(Authority.MANAGE_VISITOR);
            given(userRepository.findById(ADMIN_ID)).willReturn(Optional.of(admin));
        }

        @Test
        @DisplayName("MyVisitDetailResponseDto 타입으로 반환한다.")
        void it_returns_my_visit_detail_response_dto() {
            // given
            User host = createHost();
            Visit visit =
                    Visit.builder()
                            .id(VISIT_ID)
                            .password(ENCODED_PASSWORD)
                            .status(VisitStatus.IN_PROGRESS)
                            .visitCategory(VisitCategory.PRE_ONE_DAY)
                            .records(new ArrayList<>())
                            .build();
            given(visitRepository.findById(VISIT_ID)).willReturn(Optional.of(visit));

            // when
            Object result = visitService.getVisitDetailForAdmin(ADMIN_ID, VISIT_ID);

            // then
            assertThat(result)
                    .isInstanceOf(
                            kr.co.awesomelead.groupware_backend.domain.visit.dto.response
                                    .MyVisitDetailResponseDto.class);
        }

        @Test
        @DisplayName("records에 퇴실 시간 마지막 등록/수정자 정보를 포함한다.")
        void it_returns_exit_time_updated_by_for_each_record() {
            // given
            User manager =
                    User.builder().id(ADMIN_ID).nameKor("김관리").position(Position.MANAGER).build();
            manager.setDepartment(createDepartment(1L));

            VisitRecord record =
                    VisitRecord.builder()
                            .id(10L)
                            .entryTime(LocalDateTime.of(2026, 1, 22, 10, 0))
                            .exitTime(LocalDateTime.of(2026, 1, 22, 18, 0))
                            .exitTimeUpdatedBy(manager)
                            .build();

            Visit visit = createBaseVisit(VisitStatus.COMPLETED, VisitCategory.PRE_ONE_DAY);
            visit.getRecords().add(record);
            given(visitRepository.findById(VISIT_ID)).willReturn(Optional.of(visit));

            // when
            MyVisitDetailResponseDto result =
                    visitService.getVisitDetailForAdmin(ADMIN_ID, VISIT_ID);

            // then
            assertThat(result.getRecords()).hasSize(1);
            assertThat(result.getRecords().get(0).getExitTimeUpdatedBy()).isNotNull();
            assertThat(result.getRecords().get(0).getExitTimeUpdatedBy().getUserId())
                    .isEqualTo(ADMIN_ID);
            assertThat(result.getRecords().get(0).getExitTimeUpdatedBy().getName())
                    .isEqualTo("김관리");
            assertThat(result.getRecords().get(0).getExitTimeUpdatedBy().getDepartmentName())
                    .isEqualTo(DepartmentName.SALES_DEPT);
            assertThat(result.getRecords().get(0).getExitTimeUpdatedBy().getPosition())
                    .isEqualTo(Position.MANAGER);
        }
    }

    // =========================================================
    // A. 다중 VisitHost 생성 테스트
    // =========================================================

    @Nested
    @DisplayName("registerOneDayPreVisit - 다중 VisitHost 생성")
    class Describe_registerOneDayPreVisit_multipleHosts {

        @Nested
        @DisplayName("hostIds에 2개의 ID를 전달하면")
        class Context_with_two_host_ids {

            @Test
            @DisplayName("Visit에 VisitHost가 2개 생성되어 저장된다.")
            void registerOneDayPreVisit_twoHostIdsCreatingTwoVisitHosts() {
                // given
                Long hostId1 = 1L;
                Long hostId2 = 2L;

                Department dept = createDepartment(10L);

                User host1 =
                        User.builder()
                                .id(hostId1)
                                .nameKor("담당자1")
                                .workLocation(Company.AWESOME)
                                .jobType(JobType.MANAGEMENT)
                                .department(dept)
                                .build();
                host1.addAuthority(Authority.MANAGE_VISITOR);

                User host2 =
                        User.builder()
                                .id(hostId2)
                                .nameKor("담당자2")
                                .workLocation(Company.AWESOME)
                                .jobType(JobType.MANAGEMENT)
                                .department(dept)
                                .build();
                host2.addAuthority(Authority.MANAGE_VISITOR);

                OneDayVisitRequestDto dto =
                        OneDayVisitRequestDto.builder()
                                .visitorName("홍길동")
                                .visitorPhoneNumber("01012345678")
                                .visitorCompany("테스트컴퍼니")
                                .purpose(VisitPurpose.MEETING)
                                .permissionType(AdditionalPermissionType.NONE)
                                .visitDate(LocalDate.now().plusDays(1))
                                .plannedEntryTime(LocalTime.of(10, 0))
                                .plannedExitTime(LocalTime.of(18, 0))
                                .hostIds(List.of(hostId1, hostId2))
                                .password("1234")
                                .build();

                given(userRepository.findById(hostId1)).willReturn(Optional.of(host1));
                given(userRepository.findById(hostId2)).willReturn(Optional.of(host2));
                given(passwordEncoder.encode(dto.getPassword())).willReturn(ENCODED_PASSWORD);

                Visit savedVisit =
                        Visit.builder()
                                .id(VISIT_ID)
                                .password(ENCODED_PASSWORD)
                                .status(VisitStatus.NOT_VISITED)
                                .visitCategory(VisitCategory.PRE_ONE_DAY)
                                .records(new ArrayList<>())
                                .build();
                given(visitMapper.toOneDayVisit(any(), any(), any())).willReturn(savedVisit);
                given(visitRepository.save(any(Visit.class))).willReturn(savedVisit);

                // when
                visitService.registerOneDayPreVisit(dto);

                // then
                verify(visitRepository).save(argThat(visit -> visit.getHosts().size() == 2));
            }
        }
    }

    // =========================================================
    // B. 복수 담당자 부서에 알림 발송 테스트
    // =========================================================

    @Nested
    @DisplayName("registerOneDayPreVisit - 복수 담당자 부서 알림 발송")
    class Describe_registerOneDayPreVisit_departmentNotification {

        @Nested
        @DisplayName("두 담당자가 서로 다른 부서에 속할 때")
        class Context_with_two_hosts_in_different_departments {

            @Test
            @DisplayName("sendVisitAlertToDepartment가 2번 호출된다.")
            void registerOneDayPreVisit_differentDepartments_sendAlertTwice() {
                // given
                Long hostId1 = 1L;
                Long hostId2 = 2L;

                Department dept1 = createDepartment(10L);
                Department dept2 =
                        Department.builder().id(20L).name(DepartmentName.SALES_DEPT).build();

                User host1 =
                        User.builder()
                                .id(hostId1)
                                .nameKor("담당자1")
                                .workLocation(Company.AWESOME)
                                .jobType(JobType.MANAGEMENT)
                                .department(dept1)
                                .build();
                host1.addAuthority(Authority.MANAGE_VISITOR);

                User host2 =
                        User.builder()
                                .id(hostId2)
                                .nameKor("담당자2")
                                .workLocation(Company.AWESOME)
                                .jobType(JobType.MANAGEMENT)
                                .department(dept2)
                                .build();
                host2.addAuthority(Authority.MANAGE_VISITOR);

                OneDayVisitRequestDto dto =
                        OneDayVisitRequestDto.builder()
                                .visitorName("홍길동")
                                .visitorPhoneNumber("01012345678")
                                .visitorCompany("테스트컴퍼니")
                                .purpose(VisitPurpose.MEETING)
                                .permissionType(AdditionalPermissionType.NONE)
                                .visitDate(LocalDate.now().plusDays(1))
                                .plannedEntryTime(LocalTime.of(10, 0))
                                .plannedExitTime(LocalTime.of(18, 0))
                                .hostIds(List.of(hostId1, hostId2))
                                .password("1234")
                                .build();

                given(userRepository.findById(hostId1)).willReturn(Optional.of(host1));
                given(userRepository.findById(hostId2)).willReturn(Optional.of(host2));
                given(passwordEncoder.encode(any())).willReturn(ENCODED_PASSWORD);

                Visit savedVisit =
                        Visit.builder()
                                .id(VISIT_ID)
                                .password(ENCODED_PASSWORD)
                                .status(VisitStatus.NOT_VISITED)
                                .visitCategory(VisitCategory.PRE_ONE_DAY)
                                .records(new ArrayList<>())
                                .build();
                given(visitMapper.toOneDayVisit(any(), any(), any())).willReturn(savedVisit);
                given(visitRepository.save(any(Visit.class))).willReturn(savedVisit);

                // when
                visitService.registerOneDayPreVisit(dto);

                // then: 서로 다른 부서 ID(10L, 20L)로 각각 1회씩 총 2회 호출
                verify(notificationService, times(2))
                        .sendVisitAlertToDepartment(
                                any(), any(), any(), any(), any(), any(), any(), any());
            }
        }

        @Nested
        @DisplayName("두 담당자가 같은 부서에 속할 때")
        class Context_with_two_hosts_in_same_department {

            @Test
            @DisplayName("sendVisitAlertToDepartment가 1번만 호출된다 (중복 제거).")
            void registerOneDayPreVisit_sameDepartment_sendAlertOnce() {
                // given
                Long hostId1 = 1L;
                Long hostId2 = 2L;

                Department sharedDept = createDepartment(10L);

                User host1 =
                        User.builder()
                                .id(hostId1)
                                .nameKor("담당자1")
                                .workLocation(Company.AWESOME)
                                .jobType(JobType.MANAGEMENT)
                                .department(sharedDept)
                                .build();
                host1.addAuthority(Authority.MANAGE_VISITOR);

                User host2 =
                        User.builder()
                                .id(hostId2)
                                .nameKor("담당자2")
                                .workLocation(Company.AWESOME)
                                .jobType(JobType.MANAGEMENT)
                                .department(sharedDept)
                                .build();
                host2.addAuthority(Authority.MANAGE_VISITOR);

                OneDayVisitRequestDto dto =
                        OneDayVisitRequestDto.builder()
                                .visitorName("홍길동")
                                .visitorPhoneNumber("01012345678")
                                .visitorCompany("테스트컴퍼니")
                                .purpose(VisitPurpose.MEETING)
                                .permissionType(AdditionalPermissionType.NONE)
                                .visitDate(LocalDate.now().plusDays(1))
                                .plannedEntryTime(LocalTime.of(10, 0))
                                .plannedExitTime(LocalTime.of(18, 0))
                                .hostIds(List.of(hostId1, hostId2))
                                .password("1234")
                                .build();

                given(userRepository.findById(hostId1)).willReturn(Optional.of(host1));
                given(userRepository.findById(hostId2)).willReturn(Optional.of(host2));
                given(passwordEncoder.encode(any())).willReturn(ENCODED_PASSWORD);

                Visit savedVisit =
                        Visit.builder()
                                .id(VISIT_ID)
                                .password(ENCODED_PASSWORD)
                                .status(VisitStatus.NOT_VISITED)
                                .visitCategory(VisitCategory.PRE_ONE_DAY)
                                .records(new ArrayList<>())
                                .build();
                given(visitMapper.toOneDayVisit(any(), any(), any())).willReturn(savedVisit);
                given(visitRepository.save(any(Visit.class))).willReturn(savedVisit);

                // when
                visitService.registerOneDayPreVisit(dto);

                // then: 같은 부서이므로 중복 제거 후 1회만 호출
                verify(notificationService, times(1))
                        .sendVisitAlertToDepartment(
                                any(), any(), any(), any(), any(), any(), any(), any());
            }
        }
    }

    // =========================================================
    // C. 환경안전부 조건부 알림 테스트
    // =========================================================

    // =========================================================
    // D. 알림 발송 중복 제거 테스트
    // =========================================================

    @Nested
    @DisplayName("registerOneDayPreVisit - 알림 발송 중복 제거")
    class Describe_registerOneDayPreVisit_deduplicateAlerts {

        @Nested
        @DisplayName("같은 부서(dept ID=1L) 소속 담당자가 2명일 때")
        class Context_with_two_hosts_in_same_dept {

            @Test
            @DisplayName("sendVisitAlertToDepartment가 해당 부서로 정확히 1회만 호출된다.")
            void registerOneDayPreVisit_sameDept_sendAlertOnce() {
                // given
                Long deptId = 1L;
                Department sharedDept = createDepartment(deptId);

                User host1 =
                        User.builder()
                                .id(10L)
                                .nameKor("담당자1")
                                .workLocation(Company.AWESOME)
                                .jobType(JobType.MANAGEMENT)
                                .department(sharedDept)
                                .build();
                host1.addAuthority(Authority.MANAGE_VISITOR);

                User host2 =
                        User.builder()
                                .id(11L)
                                .nameKor("담당자2")
                                .workLocation(Company.AWESOME)
                                .jobType(JobType.MANAGEMENT)
                                .department(sharedDept)
                                .build();
                host2.addAuthority(Authority.MANAGE_VISITOR);

                OneDayVisitRequestDto dto =
                        OneDayVisitRequestDto.builder()
                                .visitorName("홍길동")
                                .visitorPhoneNumber("01012345678")
                                .visitorCompany("테스트컴퍼니")
                                .purpose(VisitPurpose.MEETING)
                                .permissionType(AdditionalPermissionType.NONE)
                                .visitDate(LocalDate.now().plusDays(1))
                                .plannedEntryTime(LocalTime.of(10, 0))
                                .plannedExitTime(LocalTime.of(18, 0))
                                .hostIds(List.of(10L, 11L))
                                .password("1234")
                                .build();

                given(userRepository.findById(10L)).willReturn(Optional.of(host1));
                given(userRepository.findById(11L)).willReturn(Optional.of(host2));
                given(passwordEncoder.encode(any())).willReturn(ENCODED_PASSWORD);

                Visit savedVisit =
                        Visit.builder()
                                .id(VISIT_ID)
                                .password(ENCODED_PASSWORD)
                                .status(VisitStatus.NOT_VISITED)
                                .visitCategory(VisitCategory.PRE_ONE_DAY)
                                .records(new ArrayList<>())
                                .build();
                given(visitMapper.toOneDayVisit(any(), any(), any())).willReturn(savedVisit);
                given(visitRepository.save(any(Visit.class))).willReturn(savedVisit);

                // when
                visitService.registerOneDayPreVisit(dto);

                // then: 같은 부서이므로 중복 제거 후 deptId=1L로 정확히 1회만 호출
                verify(notificationService, times(1))
                        .sendVisitAlertToDepartment(
                                any(), any(), eq(deptId), any(), any(), any(), any(), any());
            }
        }

        @Nested
        @DisplayName("담당자가 환경안전부(dept ID=99L) 소속이고 방문 목적이 FACILITY_CONSTRUCTION일 때")
        class Context_with_env_safety_host_and_facility_construction_purpose {

            @Test
            @DisplayName("sendVisitAlertToDepartment가 환경안전부(99L)로 정확히 1회만 호출된다.")
            void registerOneDayPreVisit_envSafetyHostWithFacilityConstruction_sendAlertOnce() {
                // given
                Long envSafetyDeptId = 99L;
                Department envSafetyDept =
                        Department.builder()
                                .id(envSafetyDeptId)
                                .name(DepartmentName.ENVIRONMENT_SAFETY)
                                .build();

                User host =
                        User.builder()
                                .id(20L)
                                .nameKor("환경안전담당자")
                                .workLocation(Company.AWESOME)
                                .jobType(JobType.MANAGEMENT)
                                .department(envSafetyDept)
                                .build();
                host.addAuthority(Authority.MANAGE_VISITOR);

                OneDayVisitRequestDto dto =
                        OneDayVisitRequestDto.builder()
                                .visitorName("홍길동")
                                .visitorPhoneNumber("01012345678")
                                .visitorCompany("테스트컴퍼니")
                                .purpose(VisitPurpose.FACILITY_CONSTRUCTION)
                                .permissionType(AdditionalPermissionType.CONFINED_SPACE_ENTRY)
                                .visitDate(LocalDate.now().plusDays(1))
                                .plannedEntryTime(LocalTime.of(10, 0))
                                .plannedExitTime(LocalTime.of(18, 0))
                                .hostIds(List.of(20L))
                                .password("1234")
                                .build();

                given(userRepository.findById(20L)).willReturn(Optional.of(host));
                given(passwordEncoder.encode(any())).willReturn(ENCODED_PASSWORD);
                given(departmentRepository.findByName(DepartmentName.ENVIRONMENT_SAFETY))
                        .willReturn(Optional.of(envSafetyDept));

                Visit savedVisit =
                        Visit.builder()
                                .id(VISIT_ID)
                                .password(ENCODED_PASSWORD)
                                .status(VisitStatus.NOT_VISITED)
                                .visitCategory(VisitCategory.PRE_ONE_DAY)
                                .records(new ArrayList<>())
                                .build();
                given(visitMapper.toOneDayVisit(any(), any(), any())).willReturn(savedVisit);
                given(visitRepository.save(any(Visit.class))).willReturn(savedVisit);

                // when
                visitService.registerOneDayPreVisit(dto);

                // then: 호스트 부서=환경안전부(99L), 조건부 환경안전부(99L) 모두 동일 부서이므로
                //       중복 제거 후 99L로 정확히 1회만 호출되어야 한다.
                verify(notificationService, times(1))
                        .sendVisitAlertToDepartment(
                                any(),
                                any(),
                                eq(envSafetyDeptId),
                                any(),
                                any(),
                                any(),
                                any(),
                                any());
            }
        }
    }

    @Nested
    @DisplayName("registerOneDayPreVisit - 환경안전부 조건부 알림")
    class Describe_registerOneDayPreVisit_environmentSafetyAlert {

        @Nested
        @DisplayName("방문 목적이 CUSTOMER_INSPECTION일 때")
        class Context_with_customer_inspection_purpose {

            @Test
            @DisplayName("환경안전부가 존재하면 해당 부서 ID로 sendVisitAlertToDepartment가 추가 호출된다.")
            void registerOneDayPreVisit_customerInspection_sendsEnvironmentSafetyAlert() {
                // given
                Long hostId = 1L;
                Long envSafetyDeptId = 99L;

                Department hostDept = createDepartment(10L);
                Department envSafetyDept =
                        Department.builder()
                                .id(envSafetyDeptId)
                                .name(DepartmentName.ENVIRONMENT_SAFETY)
                                .build();

                User host =
                        User.builder()
                                .id(hostId)
                                .nameKor("담당자")
                                .workLocation(Company.AWESOME)
                                .jobType(JobType.MANAGEMENT)
                                .department(hostDept)
                                .build();
                host.addAuthority(Authority.MANAGE_VISITOR);

                OneDayVisitRequestDto dto =
                        OneDayVisitRequestDto.builder()
                                .visitorName("홍길동")
                                .visitorPhoneNumber("01012345678")
                                .visitorCompany("테스트컴퍼니")
                                .purpose(VisitPurpose.CUSTOMER_INSPECTION)
                                .permissionType(AdditionalPermissionType.NONE)
                                .visitDate(LocalDate.now().plusDays(1))
                                .plannedEntryTime(LocalTime.of(10, 0))
                                .plannedExitTime(LocalTime.of(18, 0))
                                .hostIds(List.of(hostId))
                                .password("1234")
                                .build();

                given(userRepository.findById(hostId)).willReturn(Optional.of(host));
                given(passwordEncoder.encode(any())).willReturn(ENCODED_PASSWORD);
                given(departmentRepository.findByName(DepartmentName.ENVIRONMENT_SAFETY))
                        .willReturn(Optional.of(envSafetyDept));

                Visit savedVisit =
                        Visit.builder()
                                .id(VISIT_ID)
                                .password(ENCODED_PASSWORD)
                                .status(VisitStatus.NOT_VISITED)
                                .visitCategory(VisitCategory.PRE_ONE_DAY)
                                .records(new ArrayList<>())
                                .build();
                given(visitMapper.toOneDayVisit(any(), any(), any())).willReturn(savedVisit);
                given(visitRepository.save(any(Visit.class))).willReturn(savedVisit);

                // when
                visitService.registerOneDayPreVisit(dto);

                // then
                // 담당부서(10L) 1회 + 환경안전부(99L) 1회 = 총 2회
                verify(departmentRepository).findByName(DepartmentName.ENVIRONMENT_SAFETY);
                verify(notificationService, times(2))
                        .sendVisitAlertToDepartment(
                                any(), any(), any(), any(), any(), any(), any(), any());
            }
        }

        @Nested
        @DisplayName("방문 목적이 MEETING(환경안전 미해당)일 때")
        class Context_with_meeting_purpose {

            @Test
            @DisplayName("departmentRepository.findByName이 호출되지 않는다.")
            void registerOneDayPreVisit_meeting_doesNotQueryEnvironmentSafetyDept() {
                // given
                Long hostId = 1L;
                Department hostDept = createDepartment(10L);

                User host =
                        User.builder()
                                .id(hostId)
                                .nameKor("담당자")
                                .workLocation(Company.AWESOME)
                                .jobType(JobType.MANAGEMENT)
                                .department(hostDept)
                                .build();
                host.addAuthority(Authority.MANAGE_VISITOR);

                OneDayVisitRequestDto dto =
                        OneDayVisitRequestDto.builder()
                                .visitorName("홍길동")
                                .visitorPhoneNumber("01012345678")
                                .visitorCompany("테스트컴퍼니")
                                .purpose(VisitPurpose.MEETING)
                                .permissionType(AdditionalPermissionType.NONE)
                                .visitDate(LocalDate.now().plusDays(1))
                                .plannedEntryTime(LocalTime.of(10, 0))
                                .plannedExitTime(LocalTime.of(18, 0))
                                .hostIds(List.of(hostId))
                                .password("1234")
                                .build();

                given(userRepository.findById(hostId)).willReturn(Optional.of(host));
                given(passwordEncoder.encode(any())).willReturn(ENCODED_PASSWORD);

                Visit savedVisit =
                        Visit.builder()
                                .id(VISIT_ID)
                                .password(ENCODED_PASSWORD)
                                .status(VisitStatus.NOT_VISITED)
                                .visitCategory(VisitCategory.PRE_ONE_DAY)
                                .records(new ArrayList<>())
                                .build();
                given(visitMapper.toOneDayVisit(any(), any(), any())).willReturn(savedVisit);
                given(visitRepository.save(any(Visit.class))).willReturn(savedVisit);

                // when
                visitService.registerOneDayPreVisit(dto);

                // then
                verify(departmentRepository, times(0))
                        .findByName(DepartmentName.ENVIRONMENT_SAFETY);
            }
        }
    }
}
