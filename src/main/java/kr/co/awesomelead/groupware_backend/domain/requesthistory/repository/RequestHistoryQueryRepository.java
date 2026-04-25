package kr.co.awesomelead.groupware_backend.domain.requesthistory.repository;

import com.querydsl.core.types.Order;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.dsl.PathBuilder;
import com.querydsl.jpa.impl.JPAQueryFactory;

import kr.co.awesomelead.groupware_backend.domain.requesthistory.entity.QRequestHistory;
import kr.co.awesomelead.groupware_backend.domain.requesthistory.entity.RequestHistory;
import kr.co.awesomelead.groupware_backend.domain.requesthistory.enums.RequestHistoryStatus;
import kr.co.awesomelead.groupware_backend.domain.user.entity.QUser;

import lombok.RequiredArgsConstructor;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.support.PageableExecutionUtils;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;

@Repository
@RequiredArgsConstructor
public class RequestHistoryQueryRepository {

    private final JPAQueryFactory queryFactory;

    public Page<RequestHistory> findAllWithUserAndDepartmentByStatus(
            RequestHistoryStatus status, Pageable pageable) {

        QRequestHistory rh = QRequestHistory.requestHistory;
        QUser user = QUser.user;

        List<RequestHistory> content =
                queryFactory
                        .selectFrom(rh)
                        .innerJoin(rh.user, user)
                        .fetchJoin()
                        .leftJoin(user.department)
                        .fetchJoin()
                        .where(status != null ? rh.approvalStatus.eq(status) : null)
                        .orderBy(orderSpecifiers(rh, pageable))
                        .offset(pageable.getOffset())
                        .limit(pageable.getPageSize())
                        .fetch();

        return PageableExecutionUtils.getPage(
                content,
                pageable,
                () -> {
                    Long count =
                            queryFactory
                                    .select(rh.count())
                                    .from(rh)
                                    .where(status != null ? rh.approvalStatus.eq(status) : null)
                                    .fetchOne();
                    return count != null ? count : 0L;
                });
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private OrderSpecifier<?>[] orderSpecifiers(QRequestHistory rh, Pageable pageable) {
        List<OrderSpecifier<?>> orders = new ArrayList<>();

        for (Sort.Order order : pageable.getSort()) {
            Order direction = order.isAscending() ? Order.ASC : Order.DESC;
            String property = order.getProperty();

            // createdAt은 엔티티 필드명 requestDate에 매핑
            if ("createdAt".equals(property)) {
                property = "requestDate";
            }

            PathBuilder<RequestHistory> path =
                    new PathBuilder<>(RequestHistory.class, "requestHistory");
            orders.add(new OrderSpecifier(direction, path.get(property)));
        }

        if (orders.isEmpty()) {
            orders.add(rh.requestDate.desc());
            orders.add(rh.id.desc());
        }

        return orders.toArray(new OrderSpecifier[0]);
    }
}
