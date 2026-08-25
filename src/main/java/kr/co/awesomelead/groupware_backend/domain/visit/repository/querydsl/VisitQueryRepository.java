package kr.co.awesomelead.groupware_backend.domain.visit.repository.querydsl;

import static kr.co.awesomelead.groupware_backend.domain.visit.entity.QVisit.visit;

import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;

import kr.co.awesomelead.groupware_backend.domain.user.entity.QUser;
import kr.co.awesomelead.groupware_backend.domain.visit.entity.QVisitHost;
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

        QVisitHost qVisitHost = QVisitHost.visitHost;
        QUser qUser = QUser.user;

        // Count distinct visits (avoid duplicates from hosts join)
        long total =
                Optional.ofNullable(
                                queryFactory
                                        .select(visit.id.countDistinct())
                                        .from(visit)
                                        .leftJoin(visit.hosts, qVisitHost)
                                        .leftJoin(qVisitHost.user, qUser)
                                        .leftJoin(qUser.department)
                                        .where(
                                                departmentIdEq(qVisitHost, departmentId),
                                                statusEq(status),
                                                endDateGoe(startDate),
                                                startDateLoe(endDate))
                                        .fetchOne())
                        .orElse(0L);

        if (total == 0) {
            return new PageImpl<>(List.of(), pageable, 0);
        }

        // Get page of distinct visit IDs
        List<Long> visitIds =
                queryFactory
                        .selectDistinct(visit.id)
                        .from(visit)
                        .leftJoin(visit.hosts, qVisitHost)
                        .leftJoin(qVisitHost.user, qUser)
                        .leftJoin(qUser.department)
                        .where(
                                departmentIdEq(qVisitHost, departmentId),
                                statusEq(status),
                                endDateGoe(startDate),
                                startDateLoe(endDate))
                        .orderBy(visit.id.desc())
                        .offset(pageable.getOffset())
                        .limit(pageable.getPageSize())
                        .fetch();

        // Fetch full Visit entities with hosts eagerly loaded
        List<Visit> content =
                queryFactory
                        .selectDistinct(visit)
                        .from(visit)
                        .leftJoin(visit.hosts, qVisitHost)
                        .fetchJoin()
                        .leftJoin(qVisitHost.user, qUser)
                        .fetchJoin()
                        .leftJoin(qUser.department)
                        .fetchJoin()
                        .where(visit.id.in(visitIds))
                        .orderBy(visit.id.desc())
                        .fetch();

        return new PageImpl<>(content, pageable, total);
    }

    public List<Visit> findVisitsForAdminExcel(
            Long departmentId, VisitStatus status, LocalDate startDate, LocalDate endDate) {

        QVisitHost qVisitHost = QVisitHost.visitHost;
        QUser qUser = QUser.user;

        List<Long> visitIds =
                queryFactory
                        .selectDistinct(visit.id)
                        .from(visit)
                        .leftJoin(visit.hosts, qVisitHost)
                        .leftJoin(qVisitHost.user, qUser)
                        .leftJoin(qUser.department)
                        .where(
                                departmentIdEq(qVisitHost, departmentId),
                                statusEq(status),
                                endDateGoe(startDate),
                                startDateLoe(endDate))
                        .orderBy(visit.id.desc())
                        .fetch();

        if (visitIds.isEmpty()) {
            return List.of();
        }

        return queryFactory
                .selectDistinct(visit)
                .from(visit)
                .leftJoin(visit.hosts, qVisitHost)
                .fetchJoin()
                .leftJoin(qVisitHost.user, qUser)
                .fetchJoin()
                .leftJoin(qUser.department)
                .fetchJoin()
                .where(visit.id.in(visitIds))
                .orderBy(visit.id.desc())
                .fetch();
    }

    private BooleanExpression departmentIdEq(QVisitHost qVisitHost, Long departmentId) {
        return departmentId != null ? qVisitHost.user.department.id.eq(departmentId) : null;
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
