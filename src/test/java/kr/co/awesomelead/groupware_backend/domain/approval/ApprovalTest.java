package kr.co.awesomelead.groupware_backend.domain.approval;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import kr.co.awesomelead.groupware_backend.domain.approval.dto.request.ApprovalCreateRequestDto;
import kr.co.awesomelead.groupware_backend.domain.approval.dto.request.ApprovalCreateRequestDto.StepRequestDto;
import kr.co.awesomelead.groupware_backend.domain.approval.dto.request.ApprovalListRequestDto;
import kr.co.awesomelead.groupware_backend.domain.approval.dto.request.BasicApprovalCreateRequestDto;
import kr.co.awesomelead.groupware_backend.domain.approval.dto.request.CarFuelApprovalCreateRequestDto;
import kr.co.awesomelead.groupware_backend.domain.approval.dto.request.ExpenseDraftApprovalCreateRequestDto;
import kr.co.awesomelead.groupware_backend.domain.approval.dto.request.LeaveApprovalCreateRequestDto;
import kr.co.awesomelead.groupware_backend.domain.approval.dto.request.OverseasTripApprovalCreateRequestDto;
import kr.co.awesomelead.groupware_backend.domain.approval.dto.request.WelfareExpenseApprovalCreateRequestDto;
import kr.co.awesomelead.groupware_backend.domain.approval.dto.response.ApprovalDetailResponseDto;
import kr.co.awesomelead.groupware_backend.domain.approval.dto.response.ApprovalSummaryResponseDto;
import kr.co.awesomelead.groupware_backend.domain.approval.entity.Approval;
import kr.co.awesomelead.groupware_backend.domain.approval.entity.ApprovalParticipant;
import kr.co.awesomelead.groupware_backend.domain.approval.entity.ApprovalStep;
import kr.co.awesomelead.groupware_backend.domain.approval.entity.document.BasicApproval;
import kr.co.awesomelead.groupware_backend.domain.approval.entity.document.CarFuelApproval;
import kr.co.awesomelead.groupware_backend.domain.approval.entity.document.ExpenseDraftApproval;
import kr.co.awesomelead.groupware_backend.domain.approval.entity.document.LeaveApproval;
import kr.co.awesomelead.groupware_backend.domain.approval.entity.document.OverseasTripApproval;
import kr.co.awesomelead.groupware_backend.domain.approval.entity.document.WelfareExpenseApproval;
import kr.co.awesomelead.groupware_backend.domain.approval.enums.ApprovalStatus;
import kr.co.awesomelead.groupware_backend.domain.approval.enums.DocumentType;
import kr.co.awesomelead.groupware_backend.domain.approval.enums.LeaveDetailType;
import kr.co.awesomelead.groupware_backend.domain.approval.enums.LeaveType;
import kr.co.awesomelead.groupware_backend.domain.approval.enums.ParticipantType;
import kr.co.awesomelead.groupware_backend.domain.approval.mapper.ApprovalMapper;
import kr.co.awesomelead.groupware_backend.domain.approval.repository.ApprovalAttachmentRepository;
import kr.co.awesomelead.groupware_backend.domain.approval.repository.ApprovalRepository;
import kr.co.awesomelead.groupware_backend.domain.approval.repository.querydsl.ApprovalQueryRepository;
import kr.co.awesomelead.groupware_backend.domain.approval.service.ApprovalService;
import kr.co.awesomelead.groupware_backend.domain.department.entity.Department;
import kr.co.awesomelead.groupware_backend.domain.department.enums.Company;
import kr.co.awesomelead.groupware_backend.domain.department.enums.DepartmentName;
import kr.co.awesomelead.groupware_backend.domain.user.entity.User;
import kr.co.awesomelead.groupware_backend.domain.user.enums.Role;
import kr.co.awesomelead.groupware_backend.domain.user.repository.UserRepository;
import kr.co.awesomelead.groupware_backend.global.error.CustomException;
import kr.co.awesomelead.groupware_backend.global.error.ErrorCode;
import kr.co.awesomelead.groupware_backend.global.infra.s3.service.S3Service;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;

@ExtendWith(MockitoExtension.class)
@DisplayName("ApprovalService 단위 테스트")
public class ApprovalTest {

    @InjectMocks
    private ApprovalService approvalService;

    @Mock
    private ApprovalRepository approvalRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ApprovalAttachmentRepository attachmentRepository;

    @Mock
    private ApprovalQueryRepository approvalQueryRepository;

    @Mock
    private ApprovalMapper approvalMapper;

    @Mock
    private S3Service s3Service;

    private User drafter;
    private Department department;
    private final Long DRAFTER_ID = 1L;
    private final Long APPROVER_ID = 2L;

    @BeforeEach
    void setUp() {
        department = Department.builder().id(10L).name(DepartmentName.SALES_DEPT)
            .company(Company.AWESOME)
            .build();
        drafter = User.builder().id(DRAFTER_ID).nameKor("진형").department(department).build();
    }

    @Nested
    @DisplayName("결재 생성(createApproval) 로직")
    class CreateApproval {

        @Nested
        @DisplayName("성공 케이스")
        class SuccessCases {

            @BeforeEach
            void setupSuccess() {
                given(userRepository.findById(DRAFTER_ID)).willReturn(Optional.of(drafter));
                given(userRepository.findById(APPROVER_ID))
                    .willReturn(Optional.of(User.builder().id(APPROVER_ID).build()));
            }

            @Test
            @DisplayName("기본 기안문(BASIC) 상신 성공")
            void createBasicApproval_Success() {
                BasicApprovalCreateRequestDto dto = new BasicApprovalCreateRequestDto();
                setCommonFields(dto, DocumentType.BASIC);
                prepareMockAndVerify(new BasicApproval(), dto);
            }

            @Test
            @DisplayName("근태신청서(LEAVE) 상신 성공")
            void createLeaveApproval_Success() {
                LeaveApprovalCreateRequestDto dto = new LeaveApprovalCreateRequestDto();
                setCommonFields(dto, DocumentType.LEAVE);
                dto.setStartDate(LocalDateTime.now().plusDays(1));
                dto.setEndDate(LocalDateTime.now().plusDays(2));
                dto.setLeaveType(LeaveType.LEAVE);
                dto.setLeaveDetailType(LeaveDetailType.ANNUAL);
                dto.setReason("개인 연차");
                dto.setEmergencyContact("010-0000-0000");

                prepareMockAndVerify(new LeaveApproval(), dto);
            }

            @Test
            @DisplayName("차량유류정산(CAR_FUEL) 상신 성공")
            void createCarFuelApproval_Success() {
                CarFuelApprovalCreateRequestDto dto = new CarFuelApprovalCreateRequestDto();
                setCommonFields(dto, DocumentType.CAR_FUEL);
                dto.setAgreementDepartment("인사팀");
                dto.setCarTypeNumber("제네시스/123가4567");
                dto.setFuelType("휘발유");
                dto.setTotalDistanceKm(100.0);
                dto.setFuelClaimAmount(20000L);
                dto.setTotalAmount(25000L);
                dto.setBankName("국민");
                dto.setAccountNumber("111-222");
                dto.setAccountHolder("진형");
                dto.setDetails(
                    List.of(new CarFuelApprovalCreateRequestDto.CarFuelDetailRequestDto()));

                prepareMockAndVerify(new CarFuelApproval(), dto);
            }

            @Test
            @DisplayName("지출결의(EXPENSE_DRAFT) 상신 성공")
            void createExpenseDraft_Success() {
                ExpenseDraftApprovalCreateRequestDto dto = new ExpenseDraftApprovalCreateRequestDto();
                setCommonFields(dto, DocumentType.EXPENSE_DRAFT);
                dto.setDetails(
                    List.of(
                        new ExpenseDraftApprovalCreateRequestDto.ExpenseDraftDetailRequestDto()));

                prepareMockAndVerify(new ExpenseDraftApproval(), dto);
            }

            @Test
            @DisplayName("복리후생 지출결의(WELFARE_EXPENSE) 상신 성공")
            void createWelfareExpense_Success() {
                WelfareExpenseApprovalCreateRequestDto dto = new WelfareExpenseApprovalCreateRequestDto();
                setCommonFields(dto, DocumentType.WELFARE_EXPENSE);
                dto.setDetails(
                    List.of(
                        new ExpenseDraftApprovalCreateRequestDto.ExpenseDraftDetailRequestDto()));

                prepareMockAndVerify(new WelfareExpenseApproval(), dto);
            }

            @Test
            @DisplayName("국외출장정산(OVERSEAS_TRIP) 상신 성공")
            void createOverseasTrip_Success() {
                OverseasTripApprovalCreateRequestDto dto = new OverseasTripApprovalCreateRequestDto();
                setCommonFields(dto, DocumentType.OVERSEAS_TRIP);
                dto.setDestination("미국");
                dto.setCurrencyUnit("USD");
                dto.setExchangeRate(1300.0);
                dto.setAdvanceTotal(1000000L);
                dto.setDetails(
                    List.of(
                        new OverseasTripApprovalCreateRequestDto.OverseasTripExpenseDetailRequestDto()));

                prepareMockAndVerify(new OverseasTripApproval(), dto);
            }
        }

        @Nested
        @DisplayName("실패(예외) 케이스")
        @MockitoSettings(strictness = Strictness.LENIENT)
        class FailureCases {

            @BeforeEach
            void setupFailure() {
                given(userRepository.findById(DRAFTER_ID)).willReturn(Optional.of(drafter));
                given(approvalMapper.toEntity(any())).willReturn(new BasicApproval());
            }

            @Test
            @DisplayName("기안자를 찾을 수 없는 경우 USER_NOT_FOUND 예외 발생")
            void drafterNotFound_Fail() {
                given(userRepository.findById(DRAFTER_ID)).willReturn(Optional.empty());

                assertThatThrownBy(
                    () -> approvalService.createApproval(
                        new BasicApprovalCreateRequestDto(), DRAFTER_ID))
                    .isInstanceOf(CustomException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.USER_NOT_FOUND);
            }

            @Test
            @DisplayName("결재선(Steps)이 비어있는 경우 INVALID_APPROVAL_STEP 예외 발생")
            void emptySteps_Fail() {
                given(userRepository.findById(DRAFTER_ID)).willReturn(Optional.of(drafter));
                BasicApprovalCreateRequestDto dto = new BasicApprovalCreateRequestDto();
                dto.setApprovalSteps(null); // 또는 List.of()

                assertThatThrownBy(() -> approvalService.createApproval(dto, DRAFTER_ID))
                    .isInstanceOf(CustomException.class)
                    .hasFieldOrPropertyWithValue("errorCode",
                        ErrorCode.INVALID_APPROVAL_STEP);
            }

            @Test
            @DisplayName("결재자를 찾을 수 없는 경우 USER_NOT_FOUND 예외 발생")
            void approverNotFound_Fail() {
                given(userRepository.findById(DRAFTER_ID)).willReturn(Optional.of(drafter));
                given(userRepository.findById(APPROVER_ID)).willReturn(Optional.empty());

                BasicApprovalCreateRequestDto dto = new BasicApprovalCreateRequestDto();
                setCommonFields(dto, DocumentType.BASIC);

                assertThatThrownBy(() -> approvalService.createApproval(dto, DRAFTER_ID))
                    .isInstanceOf(CustomException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.USER_NOT_FOUND);
            }

            @Test
            @DisplayName("휴가 유형에 허용되지 않는 소분류 지정 시 INVALID_LEAVE_DETAIL_TYPE 예외 발생")
            void leaveWithWrongDetailType_Fail() {
                LeaveApprovalCreateRequestDto dto = new LeaveApprovalCreateRequestDto();
                setCommonFields(dto, DocumentType.LEAVE);
                dto.setStartDate(LocalDateTime.now().plusDays(1));
                dto.setEndDate(LocalDateTime.now().plusDays(2));
                dto.setLeaveType(LeaveType.LEAVE);
                dto.setLeaveDetailType(LeaveDetailType.AM); // LEAVE에 AM은 허용 안됨
                dto.setReason("테스트");

                assertThatThrownBy(() -> approvalService.createApproval(dto, DRAFTER_ID))
                    .isInstanceOf(CustomException.class)
                    .hasFieldOrPropertyWithValue(
                        "errorCode", ErrorCode.INVALID_LEAVE_DETAIL_TYPE);
            }

            @Test
            @DisplayName("휴가 유형에 소분류 미지정 시 INVALID_LEAVE_DETAIL_TYPE 예외 발생")
            void leaveWithNullDetailType_Fail() {
                LeaveApprovalCreateRequestDto dto = new LeaveApprovalCreateRequestDto();
                setCommonFields(dto, DocumentType.LEAVE);
                dto.setStartDate(LocalDateTime.now().plusDays(1));
                dto.setEndDate(LocalDateTime.now().plusDays(2));
                dto.setLeaveType(LeaveType.LEAVE);
                dto.setLeaveDetailType(null); // LEAVE는 소분류 필수
                dto.setReason("테스트");

                assertThatThrownBy(() -> approvalService.createApproval(dto, DRAFTER_ID))
                    .isInstanceOf(CustomException.class)
                    .hasFieldOrPropertyWithValue(
                        "errorCode", ErrorCode.INVALID_LEAVE_DETAIL_TYPE);
            }

            @Test
            @DisplayName("교육 유형에 소분류 지정 시 INVALID_LEAVE_DETAIL_TYPE 예외 발생")
            void educationWithDetailType_Fail() {
                LeaveApprovalCreateRequestDto dto = new LeaveApprovalCreateRequestDto();
                setCommonFields(dto, DocumentType.LEAVE);
                dto.setStartDate(LocalDateTime.now().plusDays(1));
                dto.setEndDate(LocalDateTime.now().plusDays(2));
                dto.setLeaveType(LeaveType.EDUCATION);
                dto.setLeaveDetailType(LeaveDetailType.ANNUAL); // EDUCATION은 소분류 null이어야 함
                dto.setReason("테스트");

                assertThatThrownBy(() -> approvalService.createApproval(dto, DRAFTER_ID))
                    .isInstanceOf(CustomException.class)
                    .hasFieldOrPropertyWithValue(
                        "errorCode", ErrorCode.INVALID_LEAVE_DETAIL_TYPE);
            }

            @Test
            @DisplayName("동일 결재자가 중복된 경우 DUPLICATE_APPROVER 예외 발생")
            void duplicateApprover_Fail() {
                BasicApprovalCreateRequestDto dto = new BasicApprovalCreateRequestDto();
                dto.setTitle("테스트");
                dto.setContent("본문");

                ApprovalCreateRequestDto.StepRequestDto step1 = new ApprovalCreateRequestDto.StepRequestDto();
                step1.setApproverId(APPROVER_ID);
                step1.setSequence(1);
                ApprovalCreateRequestDto.StepRequestDto step2 = new ApprovalCreateRequestDto.StepRequestDto();
                step2.setApproverId(APPROVER_ID);
                step2.setSequence(2);
                dto.setApprovalSteps(List.of(step1, step2));

                assertThatThrownBy(() -> approvalService.createApproval(dto, DRAFTER_ID))
                    .isInstanceOf(CustomException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.DUPLICATE_APPROVER);
            }
        }
    }

    @Nested
    @DisplayName("결재 승인(approveApproval) 로직")
    class ApproveApproval {

        private final Long APPROVAL_ID = 100L;
        private final Long SECOND_APPROVER_ID = 3L;
        private User approver;
        private User secondApprover;

        @BeforeEach
        void setupApprove() {
            approver = User.builder().id(APPROVER_ID).nameKor("결재자1").build();
            secondApprover = User.builder().id(SECOND_APPROVER_ID).nameKor("결재자2").build();
        }

        @Nested
        @DisplayName("성공 케이스")
        class SuccessCases {

            @Test
            @DisplayName("단일 결재선 승인 시 문서 상태가 APPROVED로 변경된다")
            void singleStepApprove_DocumentApproved() {
                // given
                BasicApproval approval = createApprovalWithSteps(
                    createStep(1L, approver, 1, ApprovalStatus.PENDING));

                given(approvalRepository.findById(APPROVAL_ID)).willReturn(Optional.of(approval));
                given(userRepository.findById(APPROVER_ID)).willReturn(Optional.of(approver));

                // when
                approvalService.approveApproval(APPROVAL_ID, APPROVER_ID, "승인합니다.");

                // then
                assertThat(approval.getSteps().get(0).getStatus())
                    .isEqualTo(ApprovalStatus.APPROVED);
                assertThat(approval.getSteps().get(0).getComment()).isEqualTo("승인합니다.");
                assertThat(approval.getSteps().get(0).getProcessedAt()).isNotNull();
                assertThat(approval.getStatus()).isEqualTo(ApprovalStatus.APPROVED);
            }

            @Test
            @DisplayName("다중 결재선에서 첫 번째 승인 시 다음 단계가 PENDING으로 전환된다")
            void multiStepApprove_NextStepBecomesPending() {
                // given
                BasicApproval approval = createApprovalWithSteps(
                    createStep(1L, approver, 1, ApprovalStatus.PENDING),
                    createStep(2L, secondApprover, 2, ApprovalStatus.WAITING));

                given(approvalRepository.findById(APPROVAL_ID)).willReturn(Optional.of(approval));
                given(userRepository.findById(APPROVER_ID)).willReturn(Optional.of(approver));

                // when
                approvalService.approveApproval(APPROVAL_ID, APPROVER_ID, "1차 승인");

                // then
                assertThat(approval.getSteps().get(0).getStatus())
                    .isEqualTo(ApprovalStatus.APPROVED);
                assertThat(approval.getSteps().get(1).getStatus())
                    .isEqualTo(ApprovalStatus.PENDING);
                assertThat(approval.getStatus()).isEqualTo(ApprovalStatus.PENDING); // 아직 전체 승인 아님
            }

            @Test
            @DisplayName("다중 결재선에서 마지막 승인 시 문서 상태가 APPROVED로 변경된다")
            void multiStepLastApprove_DocumentApproved() {
                // given
                BasicApproval approval = createApprovalWithSteps(
                    createStep(1L, approver, 1, ApprovalStatus.APPROVED), // 이미 승인
                    createStep(2L, secondApprover, 2, ApprovalStatus.PENDING) // 현재 차례
                );

                given(approvalRepository.findById(APPROVAL_ID)).willReturn(Optional.of(approval));
                given(userRepository.findById(SECOND_APPROVER_ID))
                    .willReturn(Optional.of(secondApprover));

                // when
                approvalService.approveApproval(APPROVAL_ID, SECOND_APPROVER_ID, "최종 승인");

                // then
                assertThat(approval.getSteps().get(1).getStatus())
                    .isEqualTo(ApprovalStatus.APPROVED);
                assertThat(approval.getStatus()).isEqualTo(ApprovalStatus.APPROVED);
            }
        }

        @Nested
        @DisplayName("실패(예외) 케이스")
        @MockitoSettings(strictness = Strictness.LENIENT)
        class FailureCases {

            @Test
            @DisplayName("결재 문서를 찾을 수 없는 경우 APPROVAL_NOT_FOUND 예외 발생")
            void approvalNotFound_Fail() {
                given(approvalRepository.findById(APPROVAL_ID)).willReturn(Optional.empty());

                assertThatThrownBy(
                    () -> approvalService.approveApproval(
                        APPROVAL_ID, APPROVER_ID, "승인"))
                    .isInstanceOf(CustomException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.APPROVAL_NOT_FOUND);
            }

            @Test
            @DisplayName("결재자를 찾을 수 없는 경우 USER_NOT_FOUND 예외 발생")
            void approverNotFound_Fail() {
                BasicApproval approval = createApprovalWithSteps(
                    createStep(1L, approver, 1, ApprovalStatus.PENDING));
                given(approvalRepository.findById(APPROVAL_ID)).willReturn(Optional.of(approval));
                given(userRepository.findById(APPROVER_ID)).willReturn(Optional.empty());

                assertThatThrownBy(
                    () -> approvalService.approveApproval(
                        APPROVAL_ID, APPROVER_ID, "승인"))
                    .isInstanceOf(CustomException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.USER_NOT_FOUND);
            }

            @Test
            @DisplayName("결재선에 포함되지 않은 사용자가 승인 시 NOT_APPROVER 예외 발생")
            void notApprover_Fail() {
                Long STRANGER_ID = 999L;
                User stranger = User.builder().id(STRANGER_ID).nameKor("외부인").build();

                BasicApproval approval = createApprovalWithSteps(
                    createStep(1L, approver, 1, ApprovalStatus.PENDING));
                given(approvalRepository.findById(APPROVAL_ID)).willReturn(Optional.of(approval));
                given(userRepository.findById(STRANGER_ID)).willReturn(Optional.of(stranger));

                assertThatThrownBy(
                    () -> approvalService.approveApproval(
                        APPROVAL_ID, STRANGER_ID, "승인"))
                    .isInstanceOf(CustomException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.NOT_APPROVER);
            }

            @Test
            @DisplayName("이미 처리된 결재 단계를 다시 승인 시 ALREADY_PROCESSED_STEP 예외 발생")
            void alreadyProcessedStep_Fail() {
                BasicApproval approval = createApprovalWithSteps(
                    createStep(1L, approver, 1, ApprovalStatus.APPROVED) // 이미 승인됨
                );
                given(approvalRepository.findById(APPROVAL_ID)).willReturn(Optional.of(approval));
                given(userRepository.findById(APPROVER_ID)).willReturn(Optional.of(approver));

                assertThatThrownBy(
                    () -> approvalService.approveApproval(
                        APPROVAL_ID, APPROVER_ID, "재승인 시도"))
                    .isInstanceOf(CustomException.class)
                    .hasFieldOrPropertyWithValue("errorCode",
                        ErrorCode.ALREADY_PROCESSED_STEP);
            }

            @Test
            @DisplayName("자기 순서가 아닌 결재자가 승인 시 NOT_YOUR_TURN 예외 발생")
            void notYourTurn_Fail() {
                BasicApproval approval = createApprovalWithSteps(
                    createStep(1L, approver, 1, ApprovalStatus.PENDING), // 1번이 현재 차례
                    createStep(2L, secondApprover, 2, ApprovalStatus.PENDING) // 2번은 아직
                );
                given(approvalRepository.findById(APPROVAL_ID)).willReturn(Optional.of(approval));
                given(userRepository.findById(SECOND_APPROVER_ID))
                    .willReturn(Optional.of(secondApprover));

                assertThatThrownBy(
                    () -> approvalService.approveApproval(
                        APPROVAL_ID, SECOND_APPROVER_ID, "승인"))
                    .isInstanceOf(CustomException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.NOT_YOUR_TURN);
            }
        }
    }

    @Nested
    @DisplayName("결재 반려(rejectApproval) 로직")
    class RejectApproval {

        private final Long APPROVAL_ID = 100L;
        private final Long SECOND_APPROVER_ID = 3L;
        private User approver;
        private User secondApprover;

        @BeforeEach
        void setupReject() {
            approver = User.builder().id(APPROVER_ID).nameKor("결재자1").build();
            secondApprover = User.builder().id(SECOND_APPROVER_ID).nameKor("결재자2").build();
        }

        @Nested
        @DisplayName("성공 케이스")
        class SuccessCases {

            @Test
            @DisplayName("반려 시 해당 step이 REJECTED, 문서 전체 상태가 REJECTED로 변경된다")
            void rejectApproval_DocumentRejected() {
                // given
                BasicApproval approval = createApprovalWithSteps(
                    createStep(1L, approver, 1, ApprovalStatus.PENDING));

                given(approvalRepository.findById(APPROVAL_ID)).willReturn(Optional.of(approval));
                given(userRepository.findById(APPROVER_ID)).willReturn(Optional.of(approver));

                // when
                approvalService.rejectApproval(APPROVAL_ID, APPROVER_ID, "보완 필요합니다.");

                // then
                assertThat(approval.getSteps().get(0).getStatus())
                    .isEqualTo(ApprovalStatus.REJECTED);
                assertThat(approval.getSteps().get(0).getComment()).isEqualTo("보완 필요합니다.");
                assertThat(approval.getSteps().get(0).getProcessedAt()).isNotNull();
                assertThat(approval.getStatus()).isEqualTo(ApprovalStatus.REJECTED);
            }

            @Test
            @DisplayName("다중 결재선에서 첫 번째 결재자가 반려 시 문서 전체가 즉시 REJECTED된다")
            void multiStepReject_DocumentImmediatelyRejected() {
                // given
                BasicApproval approval = createApprovalWithSteps(
                    createStep(1L, approver, 1, ApprovalStatus.PENDING),
                    createStep(2L, secondApprover, 2, ApprovalStatus.WAITING));

                given(approvalRepository.findById(APPROVAL_ID)).willReturn(Optional.of(approval));
                given(userRepository.findById(APPROVER_ID)).willReturn(Optional.of(approver));

                // when
                approvalService.rejectApproval(APPROVAL_ID, APPROVER_ID, "반려 사유");

                // then
                assertThat(approval.getSteps().get(0).getStatus())
                    .isEqualTo(ApprovalStatus.REJECTED);
                assertThat(approval.getSteps().get(1).getStatus())
                    .isEqualTo(ApprovalStatus.WAITING); // 그대로
                assertThat(approval.getStatus()).isEqualTo(ApprovalStatus.REJECTED);
            }
        }

        @Nested
        @DisplayName("실패(예외) 케이스")
        @MockitoSettings(strictness = Strictness.LENIENT)
        class FailureCases {

            @Test
            @DisplayName("결재 문서를 찾을 수 없는 경우 APPROVAL_NOT_FOUND 예외 발생")
            void approvalNotFound_Fail() {
                given(approvalRepository.findById(APPROVAL_ID)).willReturn(Optional.empty());

                assertThatThrownBy(
                    () -> approvalService.rejectApproval(
                        APPROVAL_ID, APPROVER_ID, "반려"))
                    .isInstanceOf(CustomException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.APPROVAL_NOT_FOUND);
            }

            @Test
            @DisplayName("결재선에 포함되지 않은 사용자가 반려 시 NOT_APPROVER 예외 발생")
            void notApprover_Fail() {
                Long STRANGER_ID = 999L;
                User stranger = User.builder().id(STRANGER_ID).nameKor("외부인").build();

                BasicApproval approval = createApprovalWithSteps(
                    createStep(1L, approver, 1, ApprovalStatus.PENDING));
                given(approvalRepository.findById(APPROVAL_ID)).willReturn(Optional.of(approval));
                given(userRepository.findById(STRANGER_ID)).willReturn(Optional.of(stranger));

                assertThatThrownBy(
                    () -> approvalService.rejectApproval(
                        APPROVAL_ID, STRANGER_ID, "반려"))
                    .isInstanceOf(CustomException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.NOT_APPROVER);
            }

            @Test
            @DisplayName("자기 순서가 아닌 결재자가 반려 시 NOT_YOUR_TURN 예외 발생")
            void notYourTurn_Fail() {
                BasicApproval approval = createApprovalWithSteps(
                    createStep(1L, approver, 1, ApprovalStatus.PENDING),
                    createStep(2L, secondApprover, 2, ApprovalStatus.PENDING));
                given(approvalRepository.findById(APPROVAL_ID)).willReturn(Optional.of(approval));
                given(userRepository.findById(SECOND_APPROVER_ID))
                    .willReturn(Optional.of(secondApprover));

                assertThatThrownBy(
                    () -> approvalService.rejectApproval(
                        APPROVAL_ID, SECOND_APPROVER_ID, "반려"))
                    .isInstanceOf(CustomException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.NOT_YOUR_TURN);
            }
        }
    }

    @Nested
    @DisplayName("결재 목록 조회(getApprovalList) 로직")
    class GetApprovalList {

        @Test
        @DisplayName("조건에 맞는 결재 목록을 페이징하여 반환한다")
        void getApprovalList_Success() {
            // given
            ApprovalListRequestDto condition = new ApprovalListRequestDto();
            User user = User.builder().id(DRAFTER_ID).role(Role.USER).build();

            ApprovalSummaryResponseDto mockSummary = org.mockito.Mockito
                .mock(ApprovalSummaryResponseDto.class);
            Page<ApprovalSummaryResponseDto> expectedPage = new PageImpl<>(List.of(mockSummary));

            given(userRepository.findById(DRAFTER_ID)).willReturn(Optional.of(user));
            given(approvalQueryRepository.findApprovalsByCondition(condition, DRAFTER_ID,
                Role.USER.name()))
                .willReturn(expectedPage);

            // when
            Page<ApprovalSummaryResponseDto> result = approvalService.getApprovalList(condition,
                DRAFTER_ID);

            // then
            assertThat(result).isEqualTo(expectedPage);
            verify(approvalQueryRepository).findApprovalsByCondition(condition, DRAFTER_ID,
                Role.USER.name());
        }

        @Test
        @DisplayName("존재하지 않는 사용자로 목록 조회 시 USER_NOT_FOUND 예외 발생")
        void getApprovalList_UserNotFound_Fail() {
            given(userRepository.findById(DRAFTER_ID)).willReturn(Optional.empty());

            assertThatThrownBy(
                () -> approvalService.getApprovalList(new ApprovalListRequestDto(), DRAFTER_ID))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.USER_NOT_FOUND);
        }
    }

    @Nested
    @DisplayName("결재 상세 조회(getApprovalDetail) 로직")
    class GetApprovalDetail {

        @Test
        @DisplayName("권한이 있는 경우 상세 정보를 반환한다")
        void getApprovalDetail_Success() {
            // given
            Long approvalId = 1L;
            BasicApproval approval = new BasicApproval();
            approval.setId(approvalId);
            approval.setDrafter(drafter);
            approval.setDraftDepartment(department);
            approval.setSteps(new ArrayList<>());
            approval.setParticipants(new ArrayList<>());
            approval.setAttachments(new ArrayList<>());

            User user = User.builder().id(DRAFTER_ID).role(Role.USER).build();

            given(approvalRepository.findById(approvalId)).willReturn(Optional.of(approval));
            given(userRepository.findById(DRAFTER_ID)).willReturn(Optional.of(user));

            // when
            ApprovalDetailResponseDto result = approvalService.getApprovalDetail(approvalId,
                DRAFTER_ID);

            // then
            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(approvalId);
        }

        @Test
        @DisplayName("권한이 없는 사용자가 상세 조회 시 NOT_APPROVER 예외 발생")
        void getApprovalDetail_Forbidden_Fail() {
            // given
            Long approvalId = 1L;
            User otherUser = User.builder().id(APPROVER_ID).nameKor("다른사람").build();
            BasicApproval approval = new BasicApproval();
            approval.setId(approvalId);
            approval.setDrafter(otherUser); // 내가 기안한 거 아님
            approval.setSteps(new ArrayList<>());
            approval.setParticipants(new ArrayList<>());

            User user = User.builder().id(DRAFTER_ID).role(Role.USER).build();

            given(approvalRepository.findById(approvalId)).willReturn(Optional.of(approval));
            given(userRepository.findById(DRAFTER_ID)).willReturn(Optional.of(user));

            // when & then
            assertThatThrownBy(() -> approvalService.getApprovalDetail(approvalId, DRAFTER_ID))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.NOT_APPROVER);
        }

        @Test
        @DisplayName("참조자(REFERRER)는 상신 직후(PENDING) 바로 조회 가능하다")
        void getApprovalDetail_Referrer_Success() {
            Long approvalId = 100L;
            BasicApproval approval = createApprovalWithSteps();
            approval.setStatus(ApprovalStatus.PENDING);

            User participantUser = User.builder().id(3L).nameKor("참조자").department(department)
                .role(Role.USER)
                .build();
            ApprovalParticipant referrer = ApprovalParticipant.builder()
                .user(participantUser)
                .participantType(ParticipantType.REFERRER)
                .build();
            approval.getParticipants().add(referrer);

            given(approvalRepository.findById(approvalId)).willReturn(Optional.of(approval));
            given(userRepository.findById(3L)).willReturn(Optional.of(participantUser));

            ApprovalDetailResponseDto result = approvalService.getApprovalDetail(approvalId, 3L);

            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(100L);
        }

        @Test
        @DisplayName("관리자(ADMIN)는 기안자/결재선에 없어도 상세 조회가 가능하다")
        void getApprovalDetail_Admin_Boundary_Success() {
            // given
            Long approvalId = 100L;
            User otherUser = User.builder().id(999L).nameKor("다른기안자").build();
            BasicApproval approval = createApprovalWithSteps();
            approval.setDrafter(otherUser); // 내가 기안자 아님

            User admin = User.builder().id(55L).role(Role.ADMIN).build();

            given(approvalRepository.findById(approvalId)).willReturn(Optional.of(approval));
            given(userRepository.findById(55L)).willReturn(Optional.of(admin));

            // when
            ApprovalDetailResponseDto result = approvalService.getApprovalDetail(approvalId, 55L);

            // then
            assertThat(result).isNotNull();
            verify(approvalRepository, times(1)).findById(approvalId);
        }

        @Test
        @DisplayName("마스터 관리자(MASTER_ADMIN)는 모든 문서를 정상적으로 조회할 수 있다")
        void getApprovalDetail_MasterAdmin_Boundary_Success() {
            // given
            Long approvalId = 200L;
            BasicApproval approval = createApprovalWithSteps();
            User masterAdmin = User.builder().id(77L).role(Role.MASTER_ADMIN).build();

            given(approvalRepository.findById(approvalId)).willReturn(Optional.of(approval));
            given(userRepository.findById(77L)).willReturn(Optional.of(masterAdmin));

            // when
            ApprovalDetailResponseDto result = approvalService.getApprovalDetail(approvalId, 77L);

            // then
            assertThat(result).isNotNull();
        }

        @Test
        @DisplayName("열람권자(VIEWER)는 결재 중(PENDING)에는 조회할 수 없다")
        void getApprovalDetail_Viewer_Pending_Fail() {
            Long approvalId = 100L;
            BasicApproval approval = createApprovalWithSteps();
            approval.setStatus(ApprovalStatus.PENDING);

            User participantUser = User.builder().id(3L).nameKor("열람권자").department(department)
                .role(Role.USER)
                .build();
            ApprovalParticipant viewer = ApprovalParticipant.builder()
                .user(participantUser)
                .participantType(ParticipantType.VIEWER)
                .build();
            approval.getParticipants().add(viewer);

            given(approvalRepository.findById(approvalId)).willReturn(Optional.of(approval));
            given(userRepository.findById(3L)).willReturn(Optional.of(participantUser));

            assertThatThrownBy(() -> approvalService.getApprovalDetail(approvalId, 3L))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.NOT_APPROVER);
        }

        @Test
        @DisplayName("열람권자(VIEWER)는 최종 승인(APPROVED) 후에는 조회 가능하다")
        void getApprovalDetail_Viewer_Approved_Success() {
            Long approvalId = 100L;
            BasicApproval approval = createApprovalWithSteps();
            approval.setStatus(ApprovalStatus.APPROVED);

            User participantUser = User.builder().id(3L).nameKor("열람권자").department(department)
                .role(Role.USER)
                .build();
            ApprovalParticipant viewer = ApprovalParticipant.builder()
                .user(participantUser)
                .participantType(ParticipantType.VIEWER)
                .build();
            approval.getParticipants().add(viewer);

            given(approvalRepository.findById(approvalId)).willReturn(Optional.of(approval));
            given(userRepository.findById(3L)).willReturn(Optional.of(participantUser));

            ApprovalDetailResponseDto result = approvalService.getApprovalDetail(approvalId, 3L);

            assertThat(result).isNotNull();
            assertThat(result.getStatus()).isEqualTo(ApprovalStatus.APPROVED);
        }

        @Test
        @DisplayName("내 결재 차례가 아닐 경우 상태가 IN_PROGRESS로 동적 반환된다")
        void getApprovalDetail_NotMyTurn_StatusIsInProgress() {
            Long approvalId = 100L;
            // 현재 2번 결재자의 차례(PENDING)라고 가정
            User approver2 = User.builder().id(2L).nameKor("결재자2").department(department).build();
            BasicApproval approval = createApprovalWithSteps(
                createStep(1L, approver2, 1, ApprovalStatus.PENDING));
            approval.setStatus(ApprovalStatus.PENDING); // 전체 상태는 PENDING

            // 기안자 본인이 조회하는 상황
            User drafterUser = User.builder().id(DRAFTER_ID).role(Role.USER).department(department)
                .build();

            given(approvalRepository.findById(approvalId)).willReturn(Optional.of(approval));
            given(userRepository.findById(DRAFTER_ID)).willReturn(Optional.of(drafterUser));

            ApprovalDetailResponseDto result = approvalService.getApprovalDetail(approvalId,
                DRAFTER_ID);

            assertThat(result).isNotNull();
            // 내 차례가 아니므로 IN_PROGRESS 로 보여야 함
            assertThat(result.getStatus()).isEqualTo(ApprovalStatus.IN_PROGRESS);
        }

        @Test
        @DisplayName("내 결재 차례일 경우 상태가 PENDING으로 정상 반환된다")
        void getApprovalDetail_MyTurn_StatusIsPending() {
            Long approvalId = 100L;
            // 현재 내 차례(PENDING)라고 가정
            User me = User.builder().id(APPROVER_ID).role(Role.USER).department(department).build();
            BasicApproval approval = createApprovalWithSteps(
                createStep(1L, me, 1, ApprovalStatus.PENDING));
            approval.setStatus(ApprovalStatus.PENDING); // 전체 상태는 PENDING

            given(approvalRepository.findById(approvalId)).willReturn(Optional.of(approval));
            given(userRepository.findById(APPROVER_ID)).willReturn(Optional.of(me));

            ApprovalDetailResponseDto result = approvalService.getApprovalDetail(approvalId,
                APPROVER_ID);

            assertThat(result).isNotNull();
            // 내 차례이므로 그대로 PENDING 으로 보여야 함
            assertThat(result.getStatus()).isEqualTo(ApprovalStatus.PENDING);
        }
    }

    // --- Helper Methods ---

    private void setCommonFields(ApprovalCreateRequestDto dto, DocumentType type) {
        dto.setTitle("테스트 제목");
        dto.setContent("테스트 본문");
        dto.setDocumentType(type);

        StepRequestDto step = new StepRequestDto();
        step.setApproverId(APPROVER_ID);
        step.setSequence(1);
        dto.setApprovalSteps(List.of(step));
    }

    private void prepareMockAndVerify(Approval entity, ApprovalCreateRequestDto dto) {
        entity.setSteps(new ArrayList<>());
        entity.setParticipants(new ArrayList<>());
        entity.setAttachments(new ArrayList<>());

        given(approvalMapper.toEntity(dto)).willReturn(entity);
        ArgumentCaptor<Approval> approvalCaptor = ArgumentCaptor.forClass(Approval.class);
        given(approvalRepository.save(approvalCaptor.capture()))
            .willAnswer(
                inv -> {
                    Approval a = inv.getArgument(0);
                    a.setId(1L);
                    return a;
                });

        Long id = approvalService.createApproval(dto, DRAFTER_ID);

        // 상신 결과 검증
        assertThat(id).isEqualTo(1L);
        Approval savedApproval = approvalCaptor.getValue();
        assertThat(savedApproval.getDrafter()).isEqualTo(drafter);
        assertThat(savedApproval.getDraftDepartment()).isEqualTo(department);
        assertThat(savedApproval.getRetentionPeriod())
            .isEqualTo(savedApproval.getDocumentType().getRetentionPeriod());

        // 1. 문서 번호 포맷 검증 ([문서종류] [부서명] [날짜]-[PK])
        String today = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String docTypeName = savedApproval.getDocumentType().getDescription();
        String deptName = department.getName().getDescription();
        String expectedDocNumber;
        if (DocumentType.BASIC.equals(savedApproval.getDocumentType())) {
            expectedDocNumber = String.format("%s %s-%03d", deptName, today, 1L);
        } else {
            expectedDocNumber = String.format("%s %s %s-%03d", docTypeName, deptName, today, 1L);
        }
        assertThat(savedApproval.getDocumentNumber()).isEqualTo(expectedDocNumber);

        // 2. 다형성 상세 내역 양방향 연관관계 검증
        validateBidirectionalRelations(savedApproval);

        verify(approvalRepository).save(any());
    }

    private void validateBidirectionalRelations(Approval approval) {
        if (approval instanceof CarFuelApproval carFuel && carFuel.getDetails() != null) {
            carFuel.getDetails().forEach(d -> assertThat(d.getApproval()).isEqualTo(carFuel));
        } else if (approval instanceof OverseasTripApproval overseas
            && overseas.getDetails() != null) {
            overseas.getDetails().forEach(d -> assertThat(d.getApproval()).isEqualTo(overseas));
        } else if (approval instanceof ExpenseDraftApproval expense
            && expense.getDetails() != null) {
            expense.getDetails().forEach(d -> assertThat(d.getApproval()).isEqualTo(expense));
        }
    }

    private ApprovalStep createStep(Long id, User approver, int sequence, ApprovalStatus status) {
        return ApprovalStep.builder()
            .id(id)
            .approver(approver)
            .sequence(sequence)
            .status(status)
            .build();
    }

    private BasicApproval createApprovalWithSteps(ApprovalStep... steps) {
        BasicApproval approval = new BasicApproval();
        approval.setId(100L);
        approval.setStatus(ApprovalStatus.PENDING);
        approval.setSteps(new ArrayList<>(List.of(steps)));
        approval.setParticipants(new ArrayList<>());
        approval.setAttachments(new ArrayList<>());
        approval.setDrafter(drafter);
        approval.setDraftDepartment(department);
        for (ApprovalStep step : steps) {
            step.setApproval(approval);
        }
        return approval;
    }
}
