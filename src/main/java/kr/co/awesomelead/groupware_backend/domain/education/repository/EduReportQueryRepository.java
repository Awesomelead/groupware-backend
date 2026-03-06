package kr.co.awesomelead.groupware_backend.domain.education.repository;

import static kr.co.awesomelead.groupware_backend.domain.education.entity.QEduAttendance.eduAttendance;
import static kr.co.awesomelead.groupware_backend.domain.education.entity.QEduReport.eduReport;

import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQueryFactory;

import kr.co.awesomelead.groupware_backend.domain.department.entity.Department;
import kr.co.awesomelead.groupware_backend.domain.education.dto.response.EduReportSummaryDto;
import kr.co.awesomelead.groupware_backend.domain.education.enums.EduType;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class EduReportQueryRepository {

    private final JPAQueryFactory queryFactory;

    /**
     * 교육 보고서 목록 조회
     *
     * @param type      교육 유형 필터 (null 이면 전체)
     * @param dept      부서 필터 엔티티
     * @param userId    출석 여부 서브쿼리에 사용할 현재 사용자 ID
     * @param hasAccess ACCESS_EDUCATION 권한 보유 여부
     */
    public List<EduReportSummaryDto> findEduReports(
            EduType type, Department dept, Long userId, boolean hasAccess) {

        return queryFactory
                .select(
                        Projections.constructor(
                                EduReportSummaryDto.class,
                                eduReport.id,
                                eduReport.title,
                                eduReport.eduType,
                                eduReport.eduDate,
                                JPAExpressions.selectOne()
                                        .from(eduAttendance)
                                        .where(
                                                eduAttendance.eduReport.eq(eduReport),
                                                eduAttendance.user.id.eq(userId))
                                        .exists(),
                                eduReport.pinned))
                .from(eduReport)
                .where(eqEduType(type), deptFilter(hasAccess, dept))
                .orderBy(eduReport.pinned.desc(), eduReport.eduDate.desc())
                .fetch();
    }

    // ── BooleanExpression 모듈 ──────────────────────────────────────

    /** 교육 유형 필터 — null 이면 조건 없음 (전체) */
    private BooleanExpression eqEduType(EduType type) {
        return type != null ? eduReport.eduType.eq(type) : null;
    }

    /**
     * 부서 접근 필터
     *
     * <ul>
     * <li>hasAccess=true + dept=null → 조건 없음 (전체 조회)
     * <li>hasAccess=true + dept≠null → 해당 부서 교육만 조회
     * <li>hasAccess=false → 기존 로직: DEPARTMENT 타입이 아니거나, 타입이 DEPARTMENT이면 자신의 부서만
     * </ul>
     */
    private BooleanExpression deptFilter(boolean hasAccess, Department dept) {
        if (hasAccess) {
            // ACCESS_EDUCATION 권한 있음: dept 지정 시 해당 부서만, 없으면 전체
            return dept != null ? eduReport.department.eq(dept) : null;
        }
        // ACCESS_EDUCATION 권한 없음: 기존 로직 유지
        // - DEPARTMENT 타입이 아닌 교육(PSM, SAFETY)은 모두 보임
        // - DEPARTMENT 타입이면 자신의 부서 교육만 보임
        return eduReport.eduType
                .ne(EduType.DEPARTMENT)
                .or(eduReport.department.eq(dept));
    }
}
