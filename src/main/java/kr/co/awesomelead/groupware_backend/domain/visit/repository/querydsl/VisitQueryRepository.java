package kr.co.awesomelead.groupware_backend.domain.visit.repository.querydsl;

import static kr.co.awesomelead.groupware_backend.domain.visit.entity.QVisit.visit;

import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;

import kr.co.awesomelead.groupware_backend.domain.visit.entity.Visit;
import kr.co.awesomelead.groupware_backend.domain.visit.enums.VisitStatus;

import lombok.RequiredArgsConstructor;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class VisitQueryRepository {

    private final JPAQueryFactory queryFactory;

    public Page<Visit> findVisitsForAdmin(
            Long departmentId,
            VisitStatus status,
            LocalDate startDate,
            LocalDate endDate,
            Pageable pageable) {

        List<Visit> content =
                queryFactory
                        .selectFrom(visit)
                        .join(visit.user).fetchJoin()
                        .join(visit.user.department).fetchJoin()
                        .where(
                                departmentIdEq(departmentId),
                                statusEq(status),
                                endDateGoe(startDate),
                                startDateLoe(endDate))
                        .orderBy(visit.id.desc())
                        .offset(pageable.getOffset())
                        .limit(pageable.getPageSize())
                        .fetch();

        long total =
                Optional.ofNullable(
                                queryFactory
                                        .select(visit.count())
                                        .from(visit)
                                        .join(visit.user)
                                        .join(visit.user.department)
                                        .where(
                                                departmentIdEq(departmentId),
                                                statusEq(status),
                                                endDateGoe(startDate),
                                                startDateLoe(endDate))
                                        .fetchOne())
                        .orElse(0L);

        return new PageImpl<>(content, pageable, total);
    }

    private BooleanExpression departmentIdEq(Long departmentId) {
        return departmentId != null ? visit.user.department.id.eq(departmentId) : null;
    }

    private BooleanExpression statusEq(VisitStatus status) {
        return status != null ? visit.status.eq(status) : null;
    }

    private BooleanExpression endDateGoe(LocalDate startDate) {
        return startDate != null ? visit.endDate.goe(startDate) : null;
    }

    private BooleanExpression startDateLoe(LocalDate endDate) {
        return endDate != null ? visit.startDate.loe(endDate) : null;
    }
}
