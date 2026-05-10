package kr.co.awesomelead.groupware_backend.domain.user.repository.querydsl;

import static kr.co.awesomelead.groupware_backend.domain.user.entity.QUser.user;

import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;

import kr.co.awesomelead.groupware_backend.domain.department.entity.QDepartment;
import kr.co.awesomelead.groupware_backend.domain.user.entity.User;
import kr.co.awesomelead.groupware_backend.domain.user.enums.JobType;
import kr.co.awesomelead.groupware_backend.domain.user.enums.Position;
import kr.co.awesomelead.groupware_backend.domain.user.enums.Role;
import kr.co.awesomelead.groupware_backend.domain.user.enums.Status;

import lombok.RequiredArgsConstructor;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.support.PageableExecutionUtils;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class UserQueryRepository {

    private final JPAQueryFactory queryFactory;

    public Page<User> findAllAvailableWithFilters(
            String keyword,
            Position position,
            Long departmentId,
            JobType jobType,
            Role role,
            Pageable pageable) {

        List<User> content =
                queryFactory
                        .selectFrom(user)
                        .leftJoin(user.department, QDepartment.department)
                        .fetchJoin()
                        .where(
                                user.status.eq(Status.AVAILABLE),
                                keywordFilter(keyword),
                                positionFilter(position),
                                departmentFilter(departmentId),
                                jobTypeFilter(jobType),
                                roleFilter(role))
                        .orderBy(user.id.desc())
                        .offset(pageable.getOffset())
                        .limit(pageable.getPageSize())
                        .fetch();

        return PageableExecutionUtils.getPage(
                content,
                pageable,
                () ->
                        queryFactory
                                .select(user.count())
                                .from(user)
                                .where(
                                        user.status.eq(Status.AVAILABLE),
                                        keywordFilter(keyword),
                                        positionFilter(position),
                                        departmentFilter(departmentId),
                                        jobTypeFilter(jobType),
                                        roleFilter(role))
                                .fetchOne());
    }

    private BooleanExpression keywordFilter(String keyword) {
        if (!StringUtils.hasText(keyword)) {
            return null;
        }
        String pattern = "%" + keyword.toLowerCase() + "%";
        return user.nameKor
                .lower()
                .like(pattern)
                .or(user.nameEng.lower().like(pattern))
                .or(user.email.lower().like(pattern));
    }

    private BooleanExpression positionFilter(Position position) {
        return position != null ? user.position.eq(position) : null;
    }

    private BooleanExpression departmentFilter(Long departmentId) {
        return departmentId != null ? user.department.id.eq(departmentId) : null;
    }

    private BooleanExpression jobTypeFilter(JobType jobType) {
        return jobType != null ? user.jobType.eq(jobType) : null;
    }

    private BooleanExpression roleFilter(Role role) {
        return role != null ? user.role.eq(role) : null;
    }
}
