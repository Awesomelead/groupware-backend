package kr.co.awesomelead.groupware_backend.domain.approval;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import kr.co.awesomelead.groupware_backend.domain.approval.dto.request.ApprovalConfigSaveRequestDto;
import kr.co.awesomelead.groupware_backend.domain.approval.dto.response.ApprovalConfigResponseDto;
import kr.co.awesomelead.groupware_backend.domain.approval.entity.ApprovalLineConfig;
import kr.co.awesomelead.groupware_backend.domain.approval.enums.DocumentType;
import kr.co.awesomelead.groupware_backend.domain.approval.repository.ApprovalLineConfigRepository;
import kr.co.awesomelead.groupware_backend.domain.approval.service.ApprovalConfigService;
import kr.co.awesomelead.groupware_backend.domain.user.entity.User;
import kr.co.awesomelead.groupware_backend.domain.user.enums.Authority;
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

import java.util.List;
import java.util.Optional;

@ExtendWith(MockitoExtension.class)
@DisplayName("ApprovalConfigService 단위 테스트")
class ApprovalConfigServiceTest {

    @InjectMocks private ApprovalConfigService approvalConfigService;

    @Mock private ApprovalLineConfigRepository approvalLineConfigRepository;

    @Mock private UserRepository userRepository;

    private static final Long ADMIN_ID = 1L;

    private User adminUser;
    private User normalUser;

    @BeforeEach
    void setUp() {
        adminUser = User.builder().id(ADMIN_ID).build();
        adminUser.addAuthority(Authority.MANAGE_APPROVAL_LINE);

        normalUser = User.builder().id(2L).build();
    }

    @Nested
    @DisplayName("설정 저장 (saveConfig)")
    class SaveConfig {

        @Nested
        @DisplayName("성공 케이스")
        class SuccessCases {

            @Test
            @DisplayName("기존 설정이 없으면 새로 생성하고 저장한다")
            void saveConfig_newEntry_savesEntity() {
                ApprovalConfigSaveRequestDto request = new ApprovalConfigSaveRequestDto();
                request.setDocumentType(DocumentType.BASIC);
                request.setApproverIds(List.of(10L, 20L, 30L));
                request.setReferrerIds(List.of(40L));

                given(userRepository.findById(ADMIN_ID)).willReturn(Optional.of(adminUser));
                given(approvalLineConfigRepository.findById(DocumentType.BASIC))
                        .willReturn(Optional.empty());

                ApprovalConfigResponseDto response =
                        approvalConfigService.saveConfig(request, ADMIN_ID);

                verify(approvalLineConfigRepository).save(any(ApprovalLineConfig.class));
                assertThat(response.getDocumentType()).isEqualTo(DocumentType.BASIC);
                assertThat(response.getApproverIds()).containsExactly(10L, 20L, 30L);
                assertThat(response.getReferrerIds()).containsExactly(40L);
            }

            @Test
            @DisplayName("기존 설정이 있으면 덮어쓰고 save를 호출하지 않는다")
            void saveConfig_existingEntry_updatesInPlace() {
                ApprovalLineConfig existing =
                        ApprovalLineConfig.of(DocumentType.BASIC, List.of(1L, 2L), List.of(3L));

                ApprovalConfigSaveRequestDto request = new ApprovalConfigSaveRequestDto();
                request.setDocumentType(DocumentType.BASIC);
                request.setApproverIds(List.of(10L, 20L));
                request.setReferrerIds(List.of(99L));

                given(userRepository.findById(ADMIN_ID)).willReturn(Optional.of(adminUser));
                given(approvalLineConfigRepository.findById(DocumentType.BASIC))
                        .willReturn(Optional.of(existing));

                ApprovalConfigResponseDto response =
                        approvalConfigService.saveConfig(request, ADMIN_ID);

                verify(approvalLineConfigRepository, never()).save(any());
                assertThat(response.getApproverIds()).containsExactly(10L, 20L);
                assertThat(response.getReferrerIds()).containsExactly(99L);
            }

            @Test
            @DisplayName("결재자 순서가 정확히 유지된다")
            void saveConfig_approverOrderIsPreserved() {
                ApprovalConfigSaveRequestDto request = new ApprovalConfigSaveRequestDto();
                request.setDocumentType(DocumentType.LEAVE);
                request.setApproverIds(List.of(5L, 3L, 7L, 1L));
                request.setReferrerIds(List.of());

                given(userRepository.findById(ADMIN_ID)).willReturn(Optional.of(adminUser));
                given(approvalLineConfigRepository.findById(DocumentType.LEAVE))
                        .willReturn(Optional.empty());

                ApprovalConfigResponseDto response =
                        approvalConfigService.saveConfig(request, ADMIN_ID);

                assertThat(response.getApproverIds()).containsExactly(5L, 3L, 7L, 1L);
            }
        }

        @Nested
        @DisplayName("실패 케이스")
        class FailureCases {

            @Test
            @DisplayName("MANAGE_APPROVAL_LINE 권한이 없으면 403 예외가 발생한다")
            void saveConfig_withoutAuthority_throwsForbidden() {
                ApprovalConfigSaveRequestDto request = new ApprovalConfigSaveRequestDto();
                request.setDocumentType(DocumentType.BASIC);
                request.setApproverIds(List.of(1L));
                request.setReferrerIds(List.of());

                given(userRepository.findById(2L)).willReturn(Optional.of(normalUser));

                assertThatThrownBy(() -> approvalConfigService.saveConfig(request, 2L))
                        .isInstanceOf(CustomException.class)
                        .satisfies(
                                ex ->
                                        assertThat(((CustomException) ex).getErrorCode())
                                                .isEqualTo(
                                                        ErrorCode
                                                                .NO_AUTHORITY_FOR_APPROVAL_CONFIG));
            }
        }
    }

    @Nested
    @DisplayName("전체 설정 조회 (getAllConfigs)")
    class GetAllConfigs {

        @Test
        @DisplayName("DocumentType 전체 개수만큼 리스트가 반환되고, 설정 없는 타입은 빈 목록이다")
        void getAllConfigs_성공() {
            ApprovalLineConfig basicConfig =
                    ApprovalLineConfig.of(DocumentType.BASIC, List.of(10L, 20L), List.of(30L));

            given(approvalLineConfigRepository.findAll()).willReturn(List.of(basicConfig));

            List<ApprovalConfigResponseDto> result = approvalConfigService.getAllConfigs();

            assertThat(result).hasSize(DocumentType.values().length);
            ApprovalConfigResponseDto basicResult =
                    result.stream()
                            .filter(r -> r.getDocumentType() == DocumentType.BASIC)
                            .findFirst()
                            .orElseThrow();
            assertThat(basicResult.getApproverIds()).containsExactly(10L, 20L);
            assertThat(basicResult.getReferrerIds()).containsExactly(30L);

            result.stream()
                    .filter(r -> r.getDocumentType() != DocumentType.BASIC)
                    .forEach(
                            r -> {
                                assertThat(r.getApproverIds()).isEmpty();
                                assertThat(r.getReferrerIds()).isEmpty();
                            });
        }
    }

    @Nested
    @DisplayName("설정 조회 (getConfig)")
    class GetConfig {

        @Test
        @DisplayName("설정이 존재하면 저장된 값을 반환한다")
        void getConfig_existingConfig_returnsStoredValue() {
            ApprovalLineConfig config =
                    ApprovalLineConfig.of(
                            DocumentType.EXPENSE_DRAFT, List.of(10L, 20L), List.of(30L));

            given(approvalLineConfigRepository.findById(DocumentType.EXPENSE_DRAFT))
                    .willReturn(Optional.of(config));

            ApprovalConfigResponseDto response =
                    approvalConfigService.getConfig(DocumentType.EXPENSE_DRAFT);

            assertThat(response.getDocumentType()).isEqualTo(DocumentType.EXPENSE_DRAFT);
            assertThat(response.getApproverIds()).containsExactly(10L, 20L);
            assertThat(response.getReferrerIds()).containsExactly(30L);
        }

        @Test
        @DisplayName("설정이 없으면 빈 목록을 반환한다")
        void getConfig_noConfig_returnsEmptyLists() {
            given(approvalLineConfigRepository.findById(DocumentType.CAR_FUEL))
                    .willReturn(Optional.empty());

            ApprovalConfigResponseDto response =
                    approvalConfigService.getConfig(DocumentType.CAR_FUEL);

            assertThat(response.getDocumentType()).isEqualTo(DocumentType.CAR_FUEL);
            assertThat(response.getApproverIds()).isEmpty();
            assertThat(response.getReferrerIds()).isEmpty();
        }
    }
}
