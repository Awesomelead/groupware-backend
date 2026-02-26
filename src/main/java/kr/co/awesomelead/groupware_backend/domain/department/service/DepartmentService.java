package kr.co.awesomelead.groupware_backend.domain.department.service;

import kr.co.awesomelead.groupware_backend.domain.department.dto.response.DepartmentHierarchyResponseDto;
import kr.co.awesomelead.groupware_backend.domain.department.dto.response.OrganizationCompanyOptionResponseDto;
import kr.co.awesomelead.groupware_backend.domain.department.dto.response.OrganizationDepartmentNodeResponseDto;
import kr.co.awesomelead.groupware_backend.domain.department.dto.response.OrganizationRootTreeResponseDto;
import kr.co.awesomelead.groupware_backend.domain.department.dto.response.OrganizationUserNodeResponseDto;
import kr.co.awesomelead.groupware_backend.domain.department.dto.response.UserSummaryResponseDto;
import kr.co.awesomelead.groupware_backend.domain.department.entity.Department;
import kr.co.awesomelead.groupware_backend.domain.department.enums.Company;
import kr.co.awesomelead.groupware_backend.domain.department.enums.DepartmentName;
import kr.co.awesomelead.groupware_backend.domain.department.repository.DepartmentRepository;
import kr.co.awesomelead.groupware_backend.domain.user.entity.User;
import kr.co.awesomelead.groupware_backend.domain.user.enums.Status;
import kr.co.awesomelead.groupware_backend.domain.user.mapper.UserMapper;
import kr.co.awesomelead.groupware_backend.domain.user.repository.UserRepository;
import kr.co.awesomelead.groupware_backend.global.error.CustomException;
import kr.co.awesomelead.groupware_backend.global.error.ErrorCode;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DepartmentService {

    private final DepartmentRepository departmentRepository;
    private final UserRepository userRepository;
    private final UserMapper userMapper;

    public List<DepartmentHierarchyResponseDto> getDepartmentHierarchy(Company company) {
        // 해당 회사의 최상위 부서(parent가 null인 곳)들을 조회
        List<Department> rootDepartments =
                departmentRepository.findByParentIsNullAndCompany(company);

        // DTO로 변환 후 반환
        return rootDepartments.stream().map(DepartmentHierarchyResponseDto::from).toList();
    }

    public OrganizationRootTreeResponseDto getOrganizationTree() {
        List<Department> allRootDepartments = departmentRepository.findByParentIsNull();

        Department rootDepartment =
                allRootDepartments.stream()
                        .filter(department -> department.getName() == DepartmentName.CHUNGNAM_HQ)
                        .findFirst()
                        .orElseGet(
                                () ->
                                        allRootDepartments.stream()
                                                .sorted(Comparator.comparing(Department::getId))
                                                .findFirst()
                                                .orElseThrow(
                                                        () ->
                                                                new CustomException(
                                                                        ErrorCode.DEPARTMENT_NOT_FOUND)));

        List<OrganizationCompanyOptionResponseDto> companyOptions =
                Arrays.stream(Company.values())
                        .map(
                                company ->
                                        OrganizationCompanyOptionResponseDto.builder()
                                                .company(company)
                                                .companyName(company.getDescription())
                                                .build())
                        .toList();

        return OrganizationRootTreeResponseDto.builder()
                .rootDepartment(toOrganizationDepartmentNode(rootDepartment))
                .companyOptions(companyOptions)
                .build();
    }

    @Transactional(readOnly = true)
    public List<UserSummaryResponseDto> getUsersByDepartmentHierarchy(Long departmentId) {
        // 기준 부서 조회
        Department targetDept =
                departmentRepository
                        .findById(departmentId)
                        .orElseThrow(() -> new CustomException(ErrorCode.DEPARTMENT_NOT_FOUND));

        // 모든 하위 부서 ID 수집 (본인 포함)
        List<Long> allDeptIds = new ArrayList<>();
        collectDepartmentIdsRecursive(targetDept, allDeptIds);

        // 해당 부서들에 속한 모든 유저 조회
        List<User> users = userRepository.findAllByDepartmentIdIn(allDeptIds);

        // DTO 변환 및 반환
        return users.stream().map(userMapper::toSummaryDto).toList();
    }

    private void collectDepartmentIdsRecursive(Department department, List<Long> ids) {
        ids.add(department.getId());

        if (department.getChildren() != null) {
            for (Department child : department.getChildren()) {
                collectDepartmentIdsRecursive(child, ids);
            }
        }
    }

    private OrganizationDepartmentNodeResponseDto toOrganizationDepartmentNode(Department department) {
        List<OrganizationUserNodeResponseDto> users =
                department.getUsers().stream()
                        .filter(user -> user.getStatus() == Status.AVAILABLE)
                        .sorted(Comparator.comparing(User::getId))
                        .map(OrganizationUserNodeResponseDto::from)
                        .toList();

        List<OrganizationDepartmentNodeResponseDto> children =
                department.getChildren().stream()
                        .sorted(Comparator.comparing(Department::getId))
                        .map(this::toOrganizationDepartmentNode)
                        .toList();

        return OrganizationDepartmentNodeResponseDto.builder()
                .id(department.getId())
                .name(department.getName())
                .label(department.getName().getDescription())
                .company(department.getCompany())
                .users(users)
                .children(children)
                .build();
    }
}
