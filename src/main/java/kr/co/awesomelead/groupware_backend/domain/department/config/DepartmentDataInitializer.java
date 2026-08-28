package kr.co.awesomelead.groupware_backend.domain.department.config;

import kr.co.awesomelead.groupware_backend.domain.department.entity.Department;
import kr.co.awesomelead.groupware_backend.domain.department.enums.Company;
import kr.co.awesomelead.groupware_backend.domain.department.enums.DepartmentName;
import kr.co.awesomelead.groupware_backend.domain.department.repository.DepartmentRepository;

import lombok.RequiredArgsConstructor;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class DepartmentDataInitializer implements ApplicationRunner {

    private final DepartmentRepository departmentRepository;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        Department awesomeGroup = createOrUpdate(DepartmentName.AWESOME_GROUP, null, null);
        Department root = createOrUpdate(DepartmentName.CHUNGNAM_HQ, null, awesomeGroup);

        createOrUpdate(DepartmentName.MARUI_LAB, Company.MARUI, root);
        createOrUpdate(DepartmentName.AWESOME_LAB, Company.AWESOME, root);
        createOrUpdate(DepartmentName.SALES_DEPT, Company.AWESOME, root);

        Department planning =
                createOrUpdate(DepartmentName.CHUNGNAM_PLANNING, Company.AWESOME, root);
        Department awesomeProduction =
                createOrUpdate(DepartmentName.AWESOME_PROD_HQ, Company.AWESOME, root);
        Department maruiProduction =
                createOrUpdate(DepartmentName.MARUI_PROD_HQ, Company.MARUI, root);

        createOrUpdate(DepartmentName.TECHNICAL_ADVISOR, Company.AWESOME, root);
        createOrUpdate(DepartmentName.ENVIRONMENT_SAFETY, Company.AWESOME, root);
        createOrUpdate(DepartmentName.QUALITY_CONTROL, Company.AWESOME, root);

        createOrUpdate(DepartmentName.MANAGEMENT_SUPPORT, Company.AWESOME, planning);
        createOrUpdate(DepartmentName.CHAMBER_PROD, Company.AWESOME, awesomeProduction);
        createOrUpdate(DepartmentName.PARTS_PROD, Company.AWESOME, awesomeProduction);
        createOrUpdate(DepartmentName.PRODUCTION, Company.MARUI, maruiProduction);
    }

    private Department createOrUpdate(DepartmentName name, Company company, Department parent) {
        return departmentRepository
                .findByName(name)
                .map(department -> updateIfNeeded(department, company, parent))
                .orElseGet(() -> create(name, company, parent));
    }

    private Department create(DepartmentName name, Company company, Department parent) {
        return departmentRepository.save(
                Department.builder().name(name).company(company).parent(parent).build());
    }

    private Department updateIfNeeded(Department department, Company company, Department parent) {
        if (department.getCompany() != company) {
            department.setCompany(company);
        }
        if (!isSameParent(department.getParent(), parent)) {
            department.setParent(parent);
        }
        return department;
    }

    private boolean isSameParent(Department currentParent, Department targetParent) {
        if (currentParent == null || targetParent == null) {
            return currentParent == targetParent;
        }
        return currentParent.getId().equals(targetParent.getId());
    }
}
