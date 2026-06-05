package kr.co.awesomelead.groupware_backend.domain.department;

// --- Java Standard Libraries ---

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import kr.co.awesomelead.groupware_backend.domain.department.dto.response.DepartmentHierarchyResponseDto;
import kr.co.awesomelead.groupware_backend.domain.department.dto.response.OrganizationRootTreeResponseDto;
import kr.co.awesomelead.groupware_backend.domain.department.dto.response.UserSummaryResponseDto;
import kr.co.awesomelead.groupware_backend.domain.department.entity.Department;
import kr.co.awesomelead.groupware_backend.domain.department.enums.Company;
import kr.co.awesomelead.groupware_backend.domain.department.enums.DepartmentName;
import kr.co.awesomelead.groupware_backend.domain.department.repository.DepartmentRepository;
import kr.co.awesomelead.groupware_backend.domain.department.service.DepartmentService;
import kr.co.awesomelead.groupware_backend.domain.user.entity.User;
import kr.co.awesomelead.groupware_backend.domain.user.enums.Role;
import kr.co.awesomelead.groupware_backend.domain.user.enums.Status;
import kr.co.awesomelead.groupware_backend.domain.user.mapper.UserMapper;
import kr.co.awesomelead.groupware_backend.domain.user.repository.UserRepository;
import kr.co.awesomelead.groupware_backend.global.error.CustomException;
import kr.co.awesomelead.groupware_backend.global.error.ErrorCode;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@ExtendWith(MockitoExtension.class)
public class DepartmentServiceTest {

    @InjectMocks private DepartmentService departmentService;

    @Mock private DepartmentRepository departmentRepository;

    @Mock private UserRepository userRepository;

    @Mock private UserMapper userMapper;

    private Department rootDept;
    private Department awesomeProdDept; // 어썸리드 생산본부
    private Department chamberDept; // 챔버생산부
    private Department partDept; // 부품생산부

    @BeforeEach
    void setUp() {
        // 최상위 부서 (Level 0)
        rootDept =
                Department.builder()
                        .id(1L)
                        .name(DepartmentName.CHUNGNAM_HQ)
                        .company(Company.AWESOME)
                        .children(new ArrayList<>())
                        .build();

        // Level 1 (어썸리드 생산본부)
        awesomeProdDept =
                Department.builder()
                        .id(5L)
                        .name(DepartmentName.AWESOME_PROD_HQ)
                        .company(Company.AWESOME)
                        .parent(rootDept)
                        .children(new ArrayList<>())
                        .build();
        rootDept.getChildren().add(awesomeProdDept);

        // Level 2 (생산본부 하위 부서들)
        chamberDept =
                Department.builder()
                        .id(10L)
                        .name(DepartmentName.CHAMBER_PROD)
                        .company(Company.AWESOME)
                        .parent(awesomeProdDept)
                        .children(new ArrayList<>())
                        .build();
        partDept =
                Department.builder()
                        .id(11L)
                        .name(DepartmentName.PARTS_PROD)
                        .company(Company.AWESOME)
                        .parent(awesomeProdDept)
                        .children(new ArrayList<>())
                        .build();
        awesomeProdDept.getChildren().add(chamberDept);
        awesomeProdDept.getChildren().add(partDept);
    }

    @Test
    @DisplayName("부서 계층 구조 조회 성공 - 회사별 최상위 부서 기준")
    void getDepartmentHierarchy_Success() {
        // given
        given(departmentRepository.findByParentIsNullAndCompany(Company.AWESOME))
                .willReturn(List.of(rootDept));

        // when
        List<DepartmentHierarchyResponseDto> result =
                departmentService.getDepartmentHierarchy(Company.AWESOME);

        // then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getName()).isEqualTo(DepartmentName.CHUNGNAM_HQ);
        assertThat(result.get(0).getChildren()).hasSize(1);
    }

    @Test
    @DisplayName("부서원 통합 조회 성공 - 하위 부서 구성원을 포함하고 MASTER_ADMIN은 제외한다")
    void getUsersByDepartmentHierarchy_Success() {
        Long targetId = 5L;
        User manager = User.builder().id(1L).nameKor("이생산").department(awesomeProdDept).build();
        User chamberStaff = User.builder().id(2L).nameKor("박챔버").department(chamberDept).build();
        User partStaff = User.builder().id(3L).nameKor("김부품").department(partDept).build();
        User masterAdmin =
                User.builder()
                        .id(4L)
                        .nameKor("마스터관리자")
                        .role(Role.MASTER_ADMIN)
                        .department(partDept)
                        .build();

        given(departmentRepository.findById(targetId)).willReturn(Optional.of(awesomeProdDept));

        List<Long> expectedIds = List.of(5L, 10L, 11L);
        given(
                        userRepository.findAllByDepartmentIdIn(
                                argThat(list -> list.containsAll(expectedIds))))
                .willReturn(List.of(manager, chamberStaff, partStaff, masterAdmin));

        given(userMapper.toSummaryDto(any(User.class)))
                .willAnswer(
                        invocation -> {
                            User user = invocation.getArgument(0);
                            return UserSummaryResponseDto.builder()
                                    .id(user.getId())
                                    .name(user.getNameKor())
                                    .build();
                        });

        // when
        List<UserSummaryResponseDto> result =
                departmentService.getUsersByDepartmentHierarchy(targetId);

        // then
        assertThat(result)
                .extracting(UserSummaryResponseDto::getName)
                .containsExactly("이생산", "박챔버", "김부품");
        verify(userRepository, times(1)).findAllByDepartmentIdIn(anyList());
        verify(userMapper, times(3)).toSummaryDto(any(User.class));
    }

    @Test
    @DisplayName("부서원 통합 조회 실패 - 부서가 존재하지 않는 경우")
    void getUsersByDepartmentHierarchy_Fail_NotFound() {
        // given
        given(departmentRepository.findById(999L)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> departmentService.getUsersByDepartmentHierarchy(999L))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.DEPARTMENT_NOT_FOUND);
    }

    @Test
    @DisplayName("조직도 트리 조회 성공 - 부서 트리와 회사 전체 선택 옵션을 반환한다")
    void getOrganizationTree_Success() {
        // given
        User availableUser =
                User.builder()
                        .id(17L)
                        .nameKor("고영민")
                        .status(Status.AVAILABLE)
                        .department(chamberDept)
                        .build();
        User suspendedUser =
                User.builder()
                        .id(18L)
                        .nameKor("휴직자")
                        .status(Status.SUSPENDED)
                        .department(chamberDept)
                        .build();
        User masterAdmin =
                User.builder()
                        .id(19L)
                        .nameKor("마스터관리자")
                        .status(Status.AVAILABLE)
                        .role(Role.MASTER_ADMIN)
                        .department(chamberDept)
                        .build();
        chamberDept.setUsers(new ArrayList<>(List.of(availableUser, suspendedUser, masterAdmin)));

        given(departmentRepository.findByParentIsNull()).willReturn(List.of(rootDept));

        // when
        OrganizationRootTreeResponseDto result = departmentService.getOrganizationTree();

        // then
        assertThat(result.getRootDepartment()).isNotNull();
        assertThat(result.getRootDepartment().getCode()).isEqualTo("CHUNGNAM_HQ");
        assertThat(result.getRootDepartment().getChildren()).isNotEmpty();
        assertThat(result.getRootDepartment().getChildren().get(0).getChildren().get(0).getUsers())
                .extracting("name")
                .containsExactly("고영민");
        assertThat(result.getCompanyOptions()).hasSize(2);
        assertThat(result.getCompanyOptions().get(0).getCompany()).isEqualTo(Company.AWESOME);
        assertThat(result.getCompanyOptions().get(1).getCompany()).isEqualTo(Company.MARUI);
    }
}
