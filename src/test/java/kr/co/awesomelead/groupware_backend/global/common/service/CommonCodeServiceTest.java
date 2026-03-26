package kr.co.awesomelead.groupware_backend.global.common.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import kr.co.awesomelead.groupware_backend.domain.department.entity.Department;
import kr.co.awesomelead.groupware_backend.domain.department.enums.DepartmentName;
import kr.co.awesomelead.groupware_backend.domain.department.repository.DepartmentRepository;
import kr.co.awesomelead.groupware_backend.domain.user.enums.JobType;
import kr.co.awesomelead.groupware_backend.domain.user.enums.Position;
import kr.co.awesomelead.groupware_backend.global.common.dto.response.EnumCodeDto;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

@ExtendWith(MockitoExtension.class)
class CommonCodeServiceTest {

    @Mock private DepartmentRepository departmentRepository;

    @InjectMocks private CommonCodeService commonCodeService;

    @Test
    @DisplayName("부서 공통 코드 목록이 DB ID를 포함하여 정상 조회된다")
    void getDepartments_ReturnsDepartmentsWithId() {
        // given
        Department dept1 =
                Department.builder().id(1L).name(DepartmentName.CHUNGNAM_HQ).build();
        Department dept2 =
                Department.builder().id(2L).name(DepartmentName.PRODUCTION).build();
        when(departmentRepository.findAll()).thenReturn(List.of(dept1, dept2));

        // when
        List<EnumCodeDto> departments = commonCodeService.getDepartments();

        // then
        assertThat(departments).hasSize(2);
        assertThat(departments)
                .extracting(EnumCodeDto::getId)
                .containsExactly(1L, 2L);
        assertThat(departments)
                .extracting(EnumCodeDto::getCode)
                .containsExactly(
                        DepartmentName.CHUNGNAM_HQ.name(), DepartmentName.PRODUCTION.name());
    }

    @Test
    @DisplayName("직급 공통 코드 목록이 정상 조회된다")
    void getPositions_ReturnsPositions() {
        // given & when
        List<EnumCodeDto> positions = commonCodeService.getPositions();

        // then
        assertThat(positions).hasSize(Position.values().length);
        assertThat(positions)
                .extracting(EnumCodeDto::getCode)
                .contains(Position.CEO.name(), Position.STAFF.name());
        assertThat(positions)
                .extracting(EnumCodeDto::getId)
                .containsOnlyNulls();
    }

    @Test
    @DisplayName("직무 공통 코드 목록이 정상 조회된다")
    void getJobTypes_ReturnsJobTypes() {
        // given & when
        List<EnumCodeDto> jobTypes = commonCodeService.getJobTypes();

        // then
        assertThat(jobTypes).hasSize(JobType.values().length);
        assertThat(jobTypes)
                .extracting(EnumCodeDto::getCode)
                .contains(JobType.FIELD.name(), JobType.MANAGEMENT.name());
        assertThat(jobTypes)
                .extracting(EnumCodeDto::getId)
                .containsOnlyNulls();
    }
}
