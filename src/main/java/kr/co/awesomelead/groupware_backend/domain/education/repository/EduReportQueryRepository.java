package kr.co.awesomelead.groupware_backend.domain.education.repository;

import static kr.co.awesomelead.groupware_backend.domain.education.entity.QEduAttendance.eduAttendance;
import static kr.co.awesomelead.groupware_backend.domain.education.entity.QEduReport.eduReport;
import static kr.co.awesomelead.groupware_backend.domain.education.entity.QEducationCategory.educationCategory;

import com.querydsl.core.Tuple;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQueryFactory;

import kr.co.awesomelead.groupware_backend.domain.department.entity.Department;
import kr.co.awesomelead.groupware_backend.domain.department.enums.Company;
import kr.co.awesomelead.groupware_backend.domain.department.enums.DepartmentName;
import kr.co.awesomelead.groupware_backend.domain.education.dto.response.EduReportSummaryDto;
import kr.co.awesomelead.groupware_backend.domain.education.entity.EduReport;
import kr.co.awesomelead.groupware_backend.domain.education.entity.QEduAttendance;
import kr.co.awesomelead.groupware_backend.domain.education.enums.EduType;
import kr.co.awesomelead.groupware_backend.domain.user.entity.QUser;
import kr.co.awesomelead.groupware_backend.domain.user.enums.Position;
import kr.co.awesomelead.groupware_backend.domain.user.enums.Status;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class EduReportQueryRepository {

    private final JPAQueryFactory queryFactory;

    /**
     * 교육 보고서 목록 조회
     *
     * @param type 교육 유형 필터 (null 이면 전체)
     * @param dept 부서 필터 엔티티
     * @param userId 출석 여부 서브쿼리에 사용할 현재 사용자 ID
     * @param hasAccess MANAGE_DEPARTMENT_EDUCATION 권한 보유 여부
     */
    public List<EduReportSummaryDto> findEduReports(
            EduType type, Department dept, Long categoryId, Long userId, boolean hasAccess) {
        return findEduReports(type, dept, categoryId, userId, hasAccess, null, true);
    }

    public List<EduReportSummaryDto> findEduReports(
            EduType type,
            Department dept,
            Long categoryId,
            Long userId,
            boolean hasAccess,
            Company psmCompany,
            boolean canReadAllPsmCompanies) {

        return queryFactory
                .select(
                        Projections.constructor(
                                EduReportSummaryDto.class,
                                eduReport.id,
                                eduReport.title,
                                eduReport.eduType,
                                eduReport.eduDate,
                                eduReport.content,
                                JPAExpressions.selectOne()
                                        .from(eduAttendance)
                                        .where(
                                                eduAttendance.eduReport.eq(eduReport),
                                                eduAttendance.user.id.eq(userId))
                                        .exists(),
                                eduReport.pinned,
                                eduReport.signatureRequired,
                                eduReport.status,
                                educationCategory.id,
                                educationCategory.name))
                .from(eduReport)
                .leftJoin(eduReport.category, educationCategory)
                .where(
                        eqEduType(type),
                        eqCategoryId(categoryId),
                        deptFilter(type, hasAccess, dept),
                        psmFilter(psmCompany, canReadAllPsmCompanies))
                // 같은 eduDate(일자) 내에서는 최신 생성건이 위로 오도록 id DESC를 타이브레이커로 사용
                .orderBy(eduReport.pinned.desc(), eduReport.eduDate.desc(), eduReport.id.desc())
                .fetch();
    }

    public List<EduReportSummaryDto> findPsmEduReports(
            Long categoryId, Long userId, Company company, boolean canReadAllCompanies) {

        return queryFactory
                .select(
                        Projections.constructor(
                                EduReportSummaryDto.class,
                                eduReport.id,
                                eduReport.title,
                                eduReport.eduType,
                                eduReport.eduDate,
                                eduReport.content,
                                JPAExpressions.selectOne()
                                        .from(eduAttendance)
                                        .where(
                                                eduAttendance.eduReport.eq(eduReport),
                                                eduAttendance.user.id.eq(userId))
                                        .exists(),
                                eduReport.pinned,
                                eduReport.signatureRequired,
                                eduReport.status,
                                educationCategory.id,
                                educationCategory.name))
                .from(eduReport)
                .leftJoin(eduReport.category, educationCategory)
                .where(
                        eduReport.eduType.eq(EduType.PSM),
                        eqCategoryId(categoryId),
                        psmCompanyFilter(company, canReadAllCompanies))
                .orderBy(eduReport.pinned.desc(), eduReport.eduDate.desc(), eduReport.id.desc())
                .fetch();
    }

    public List<EduReportSummaryDto> findSafetyEduReports(
            Long categoryId, Long userId, Company company, boolean canReadAllCompanies) {

        return queryFactory
                .select(
                        Projections.constructor(
                                EduReportSummaryDto.class,
                                eduReport.id,
                                eduReport.title,
                                eduReport.eduType,
                                eduReport.eduDate,
                                eduReport.content,
                                JPAExpressions.selectOne()
                                        .from(eduAttendance)
                                        .where(
                                                eduAttendance.eduReport.eq(eduReport),
                                                eduAttendance.user.id.eq(userId))
                                        .exists(),
                                eduReport.pinned,
                                eduReport.signatureRequired,
                                eduReport.status,
                                educationCategory.id,
                                educationCategory.name))
                .from(eduReport)
                .leftJoin(eduReport.category, educationCategory)
                .where(
                        eduReport.eduType.eq(EduType.SAFETY),
                        eqCategoryId(categoryId),
                        psmCompanyFilter(company, canReadAllCompanies))
                .orderBy(eduReport.pinned.desc(), eduReport.eduDate.desc(), eduReport.id.desc())
                .fetch();
    }

    public record SignatureStatusRow(
            String nameKor,
            String nameEng,
            DepartmentName departmentName,
            Position position,
            String signatureKey) {}

    public List<SignatureStatusRow> findSignatureStatuses(EduReport report, String name) {
        QUser user = QUser.user;
        QEduAttendance att = new QEduAttendance("signatureAtt");

        List<Tuple> tuples =
                queryFactory
                        .select(
                                user.nameKor,
                                user.nameEng,
                                user.department.name,
                                user.position,
                                att.signatureKey)
                        .from(user)
                        .leftJoin(att)
                        .on(att.eduReport.id.eq(report.getId()).and(att.user.eq(user)))
                        .where(
                                user.status.eq(Status.AVAILABLE),
                                signatureMembershipFilter(report, user),
                                signatureNameFilter(name, user))
                        .orderBy(user.nameKor.asc().nullsLast(), user.nameEng.asc().nullsLast())
                        .fetch();

        return tuples.stream()
                .map(
                        t ->
                                new SignatureStatusRow(
                                        t.get(user.nameKor),
                                        t.get(user.nameEng),
                                        t.get(user.department.name),
                                        t.get(user.position),
                                        t.get(att.signatureKey)))
                .toList();
    }

    private BooleanExpression signatureMembershipFilter(EduReport report, QUser user) {
        if (report.getEduType() == EduType.DEPARTMENT) {
            if (report.getDepartment() == null) {
                return user.id.isNull(); // 결과 없음
            }
            return user.department.eq(report.getDepartment());
        }
        Company company = report.getCompany();
        if (company == null) {
            return null; // 전사 대상
        }
        return user.workLocation.eq(company);
    }

    private BooleanExpression signatureNameFilter(String name, QUser user) {
        if (!StringUtils.hasText(name)) {
            return null;
        }
        return user.nameKor
                .containsIgnoreCase(name)
                .or(user.nameEng.isNotNull().and(user.nameEng.containsIgnoreCase(name)));
    }

    // ── BooleanExpression 모듈 ──────────────────────────────────────

    /** 교육 유형 필터 — null 이면 조건 없음 (전체) */
    private BooleanExpression eqEduType(EduType type) {
        return type != null ? eduReport.eduType.eq(type) : null;
    }

    private BooleanExpression eqCategoryId(Long categoryId) {
        return categoryId != null ? educationCategory.id.eq(categoryId) : null;
    }

    /**
     * 부서 접근 필터
     *
     * <ul>
     *   <li>hasAccess=true + dept=null → 조건 없음 (전체 조회)
     *   <li>hasAccess=true + dept≠null → 해당 부서 교육만 조회
     *   <li>hasAccess=false → 기존 로직: DEPARTMENT 타입이 아니거나, 타입이 DEPARTMENT이면 자신의 부서만
     * </ul>
     */
    private BooleanExpression deptFilter(EduType type, boolean hasAccess, Department dept) {
        if (hasAccess) {
            // MANAGE_DEPARTMENT_EDUCATION 권한 있음:
            // - DEPARTMENT 조회일 때만 departmentName 필터 적용
            // - PSM/SAFETY 조회는 departmentName 무시
            if (type == EduType.DEPARTMENT && dept != null) {
                return eduReport.department.eq(dept);
            }
            return null;
        }
        // MANAGE_DEPARTMENT_EDUCATION 권한 없음: 기존 로직 유지
        // - DEPARTMENT 타입이 아닌 교육(PSM, SAFETY)은 모두 보임
        // - DEPARTMENT 타입이면 자신의 부서 교육만 보임
        return eduReport.eduType.ne(EduType.DEPARTMENT).or(eduReport.department.eq(dept));
    }

    private BooleanExpression psmCompanyFilter(Company company, boolean canReadAllCompanies) {
        if (canReadAllCompanies) {
            return null;
        }

        if (company == null) {
            return eduReport.company.isNull();
        }

        return eduReport.company.eq(company).or(eduReport.company.isNull());
    }

    private BooleanExpression psmFilter(Company psmCompany, boolean canReadAllPsmCompanies) {
        if (canReadAllPsmCompanies) {
            return null;
        }

        if (psmCompany == null) {
            return eduReport.eduType.ne(EduType.PSM).or(eduReport.company.isNull());
        }

        return eduReport
                .eduType
                .ne(EduType.PSM)
                .or(eduReport.company.eq(psmCompany))
                .or(eduReport.company.isNull());
    }
}
