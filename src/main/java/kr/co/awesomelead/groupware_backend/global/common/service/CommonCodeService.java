package kr.co.awesomelead.groupware_backend.global.common.service;

import kr.co.awesomelead.groupware_backend.domain.department.enums.DepartmentName;
import kr.co.awesomelead.groupware_backend.domain.user.enums.JobType;
import kr.co.awesomelead.groupware_backend.domain.user.enums.Position;
import kr.co.awesomelead.groupware_backend.global.common.dto.response.EnumCodeDto;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;

@Service
public class CommonCodeService {

    /**
     * 부서명 공통 코드 목록을 조회합니다.
     */
    public List<EnumCodeDto> getDepartments() {
        return Arrays.stream(DepartmentName.values())
                .map(d -> EnumCodeDto.of(d.name(), d.getDescription()))
                .toList();
    }

    /**
     * 직급 공통 코드 목록을 조회합니다.
     */
    public List<EnumCodeDto> getPositions() {
        return Arrays.stream(Position.values())
                .map(p -> EnumCodeDto.of(p.name(), p.getDescription()))
                .toList();
    }

    /**
     * 직무/직종 공통 코드 목록을 조회합니다.
     */
    public List<EnumCodeDto> getJobTypes() {
        return Arrays.stream(JobType.values())
                .map(j -> EnumCodeDto.of(j.name(), j.getDescription()))
                .toList();
    }
}
