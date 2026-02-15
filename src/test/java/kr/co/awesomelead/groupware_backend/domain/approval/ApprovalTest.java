package kr.co.awesomelead.groupware_backend.domain.approval;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import kr.co.awesomelead.groupware_backend.domain.approval.dto.request.ApprovalCreateRequestDto;
import kr.co.awesomelead.groupware_backend.domain.approval.dto.request.ApprovalCreateRequestDto.StepRequestDto;
import kr.co.awesomelead.groupware_backend.domain.approval.dto.request.BasicApprovalCreateRequestDto;
import kr.co.awesomelead.groupware_backend.domain.approval.dto.request.CarFuelApprovalCreateRequestDto;
import kr.co.awesomelead.groupware_backend.domain.approval.dto.request.ExpenseDraftApprovalCreateRequestDto;
import kr.co.awesomelead.groupware_backend.domain.approval.dto.request.LeaveApprovalCreateRequestDto;
import kr.co.awesomelead.groupware_backend.domain.approval.dto.request.OverseasTripApprovalCreateRequestDto;
import kr.co.awesomelead.groupware_backend.domain.approval.dto.request.WelfareExpenseApprovalCreateRequestDto;
import kr.co.awesomelead.groupware_backend.domain.approval.entity.Approval;
import kr.co.awesomelead.groupware_backend.domain.approval.entity.document.BasicApproval;
import kr.co.awesomelead.groupware_backend.domain.approval.entity.document.CarFuelApproval;
import kr.co.awesomelead.groupware_backend.domain.approval.entity.document.ExpenseDraftApproval;
import kr.co.awesomelead.groupware_backend.domain.approval.entity.document.LeaveApproval;
import kr.co.awesomelead.groupware_backend.domain.approval.entity.document.OverseasTripApproval;
import kr.co.awesomelead.groupware_backend.domain.approval.entity.document.WelfareExpenseApproval;
import kr.co.awesomelead.groupware_backend.domain.approval.enums.DocumentType;
import kr.co.awesomelead.groupware_backend.domain.approval.enums.LeaveDetailType;
import kr.co.awesomelead.groupware_backend.domain.approval.enums.LeaveType;
import kr.co.awesomelead.groupware_backend.domain.approval.enums.RetentionPeriod;
import kr.co.awesomelead.groupware_backend.domain.approval.mapper.ApprovalMapper;
import kr.co.awesomelead.groupware_backend.domain.approval.repository.ApprovalAttachmentRepository;
import kr.co.awesomelead.groupware_backend.domain.approval.repository.ApprovalRepository;
import kr.co.awesomelead.groupware_backend.domain.approval.service.ApprovalService;
import kr.co.awesomelead.groupware_backend.domain.department.entity.Department;
import kr.co.awesomelead.groupware_backend.domain.department.enums.DepartmentName;
import kr.co.awesomelead.groupware_backend.domain.user.entity.User;
import kr.co.awesomelead.groupware_backend.domain.user.repository.UserRepository;
import kr.co.awesomelead.groupware_backend.global.error.CustomException;
import kr.co.awesomelead.groupware_backend.global.error.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

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
    private ApprovalMapper approvalMapper;

    private User drafter;
    private Department department;
    private final Long DRAFTER_ID = 1L;
    private final Long APPROVER_ID = 2L;

    @BeforeEach
    void setUp() {
        department = Department.builder().id(10L).name(DepartmentName.SALES_DEPT).build();
        drafter = User.builder()
            .id(DRAFTER_ID)
            .nameKor("진형님")
            .department(department)
            .build();
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
                given(userRepository.findById(APPROVER_ID)).willReturn(
                    Optional.of(User.builder().id(APPROVER_ID).build()));
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
                dto.setAccountHolder("진형님");
                dto.setDetails(
                    List.of(new CarFuelApprovalCreateRequestDto.CarFuelDetailRequestDto()));

                prepareMockAndVerify(new CarFuelApproval(), dto);
            }

            @Test
            @DisplayName("지출결의(EXPENSE_DRAFT) 상신 성공")
            void createExpenseDraft_Success() {
                ExpenseDraftApprovalCreateRequestDto dto = new ExpenseDraftApprovalCreateRequestDto();
                setCommonFields(dto, DocumentType.EXPENSE_DRAFT);
                dto.setDetails(List.of(
                    new ExpenseDraftApprovalCreateRequestDto.ExpenseDraftDetailRequestDto()));

                prepareMockAndVerify(new ExpenseDraftApproval(), dto);
            }

            @Test
            @DisplayName("복리후생 지출결의(WELFARE_EXPENSE) 상신 성공")
            void createWelfareExpense_Success() {
                WelfareExpenseApprovalCreateRequestDto dto = new WelfareExpenseApprovalCreateRequestDto();
                setCommonFields(dto, DocumentType.WELFARE_EXPENSE);
                dto.setAgreementDepartment("총무팀");
                dto.setDetails(List.of(
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
                dto.setDetails(List.of(
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
                    () -> approvalService.createApproval(new BasicApprovalCreateRequestDto(),
                        DRAFTER_ID))
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
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_APPROVAL_STEP);
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
        }
    }

    // --- Helper Methods ---

    private void setCommonFields(ApprovalCreateRequestDto dto, DocumentType type) {
        dto.setTitle("테스트 제목");
        dto.setContent("테스트 본문");
        dto.setDocumentType(type);
        dto.setRetentionPeriod(RetentionPeriod.FIVE_YEARS);

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
        given(approvalRepository.save(any())).willAnswer(inv -> {
            Approval a = inv.getArgument(0);
            a.setId(1L);
            return a;
        });

        Long id = approvalService.createApproval(dto, DRAFTER_ID);

        assertThat(id).isEqualTo(1L);
        assertThat(entity.getDrafter()).isEqualTo(drafter);
        assertThat(entity.getDraftDepartment()).isEqualTo(department);
        verify(approvalRepository).save(any());
    }
}
