package kr.co.awesomelead.groupware_backend.global.common.service;

import kr.co.awesomelead.groupware_backend.domain.department.enums.DepartmentName;
import kr.co.awesomelead.groupware_backend.domain.user.enums.JobType;
import kr.co.awesomelead.groupware_backend.domain.user.enums.Position;
import kr.co.awesomelead.groupware_backend.global.common.dto.response.EnumCodeDto;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class CommonCodeServiceTest {

        private final CommonCodeService commonCodeService = new CommonCodeService();

        @Test
        @DisplayName("부서 공통 코드 목록이 정상 조회된다")
        void getDepartments_ReturnsDepartments() {
                // given & when
                List<EnumCodeDto> departments = commonCodeService.getDepartments();

                // then
                assertThat(departments).hasSize(DepartmentName.values().length);
                assertThat(departments)
                                .extracting(EnumCodeDto::getCode)
                                .contains(DepartmentName.CHUNGNAM_HQ.name(), DepartmentName.PRODUCTION.name());
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
        }
}
