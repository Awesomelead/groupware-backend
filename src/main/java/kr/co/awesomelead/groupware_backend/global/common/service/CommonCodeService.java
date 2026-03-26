package kr.co.awesomelead.groupware_backend.global.common.service;

import kr.co.awesomelead.groupware_backend.domain.department.repository.DepartmentRepository;
import kr.co.awesomelead.groupware_backend.domain.user.enums.JobType;
import kr.co.awesomelead.groupware_backend.domain.user.enums.Position;
import kr.co.awesomelead.groupware_backend.global.common.dto.response.EnumCodeDto;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CommonCodeService {

    private final DepartmentRepository departmentRepository;

    @Transactional(readOnly = true)
    public List<EnumCodeDto> getDepartments() {
        return departmentRepository.findAll().stream()
                .map(
                        d ->
                                EnumCodeDto.of(
                                        d.getId(),
                                        d.getName().name(),
                                        d.getName().getDescription()))
                .toList();
    }

    /** 직급 공통 코드 목록을 조회합니다. */
    public List<EnumCodeDto> getPositions() {
        return Arrays.stream(Position.values())
                .map(p -> EnumCodeDto.of(p.name(), p.getDescription()))
                .toList();
    }

    /** 직무/직종 공통 코드 목록을 조회합니다. */
    public List<EnumCodeDto> getJobTypes() {
        return Arrays.stream(JobType.values())
                .map(j -> EnumCodeDto.of(j.name(), j.getDescription()))
                .toList();
    }
}
