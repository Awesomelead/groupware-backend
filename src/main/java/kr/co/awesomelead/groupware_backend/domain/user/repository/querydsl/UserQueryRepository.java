package kr.co.awesomelead.groupware_backend.domain.user.repository.querydsl;

import static kr.co.awesomelead.groupware_backend.domain.user.entity.QMyInfoUpdateRequest.myInfoUpdateRequest;
import static kr.co.awesomelead.groupware_backend.domain.user.entity.QUser.user;

import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQueryFactory;

import kr.co.awesomelead.groupware_backend.domain.department.entity.QDepartment;
import kr.co.awesomelead.groupware_backend.domain.department.enums.Company;
import kr.co.awesomelead.groupware_backend.domain.user.entity.User;
import kr.co.awesomelead.groupware_backend.domain.user.enums.JobType;
import kr.co.awesomelead.groupware_backend.domain.user.enums.MyInfoUpdateRequestStatus;
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
            List<Status> statuses,
            Pageable pageable) {
        return findAllAvailableWithFilters(
                keyword, position, departmentId, jobType, role, null, statuses, pageable);
    }

    public Page<User> findAllAvailableWithFilters(
            String keyword,
            Position position,
            Long departmentId,
            JobType jobType,
            Role role,
            Company workLocation,
            List<Status> statuses,
            Pageable pageable) {

        List<User> content =
                queryFactory
                        .selectFrom(user)
                        .leftJoin(user.department, QDepartment.department)
                        .fetchJoin()
                        .where(
                                statusFilter(statuses),
                                keywordFilter(keyword),
                                positionFilter(position),
                                departmentFilter(departmentId),
                                jobTypeFilter(jobType),
                                roleFilter(role),
                                workLocationFilter(workLocation),
                                excludeMasterAdmin())
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
                                        statusFilter(statuses),
                                        keywordFilter(keyword),
                                        positionFilter(position),
                                        departmentFilter(departmentId),
                                        jobTypeFilter(jobType),
                                        roleFilter(role),
                                        workLocationFilter(workLocation),
                                        excludeMasterAdmin())
                                .fetchOne());
    }

    private BooleanExpression statusFilter(List<Status> statuses) {
        if (statuses == null || statuses.isEmpty()) {
            return user.status.in(Status.AVAILABLE, Status.SUSPENDED);
        }
        List<Status> safeStatuses = statuses.stream().filter(s -> s != Status.PENDING).toList();
        if (safeStatuses.isEmpty()) {
            return user.id.isNull();
        }
        return user.status.in(safeStatuses);
    }

    public Page<User> findAllForAdminWithFilters(
            String keyword,
            Position position,
            Long departmentId,
            JobType jobType,
            Role role,
            Company workLocation,
            List<Status> statuses,
            Boolean hasPendingMyInfoRequest,
            Pageable pageable) {
        List<User> content =
                queryFactory
                        .selectFrom(user)
                        .leftJoin(user.department, QDepartment.department)
                        .fetchJoin()
                        .where(
                                keywordFilter(keyword),
                                positionFilter(position),
                                departmentFilter(departmentId),
                                jobTypeFilter(jobType),
                                roleFilter(role),
                                workLocationFilter(workLocation),
                                adminStatusFilter(statuses),
                                pendingMyInfoRequestFilter(hasPendingMyInfoRequest),
                                excludeMasterAdmin())
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
                                        keywordFilter(keyword),
                                        positionFilter(position),
                                        departmentFilter(departmentId),
                                        jobTypeFilter(jobType),
                                        roleFilter(role),
                                        workLocationFilter(workLocation),
                                        adminStatusFilter(statuses),
                                        pendingMyInfoRequestFilter(hasPendingMyInfoRequest),
                                        excludeMasterAdmin())
                                .fetchOne());
    }

    public List<User> findAllForAdminWithFiltersNoPaging(
            String keyword,
            Position position,
            Long departmentId,
            JobType jobType,
            Role role,
            Company workLocation,
            List<Status> statuses) {
        return queryFactory
                .selectFrom(user)
                .leftJoin(user.department, QDepartment.department)
                .fetchJoin()
                .where(
                        keywordFilter(keyword),
                        positionFilter(position),
                        departmentFilter(departmentId),
                        jobTypeFilter(jobType),
                        roleFilter(role),
                        workLocationFilter(workLocation),
                        adminStatusFilter(statuses),
                        excludeMasterAdmin())
                .orderBy(user.id.desc())
                .fetch();
    }

    private BooleanExpression adminStatusFilter(List<Status> statuses) {
        if (statuses == null || statuses.isEmpty()) {
            return null;
        }
        return user.status.in(statuses);
    }

    private BooleanExpression pendingMyInfoRequestFilter(Boolean hasPendingMyInfoRequest) {
        if (hasPendingMyInfoRequest == null) {
            return null;
        }

        BooleanExpression existsPendingRequest =
                JPAExpressions.selectOne()
                        .from(myInfoUpdateRequest)
                        .where(
                                myInfoUpdateRequest.user.eq(user),
                                myInfoUpdateRequest.status.eq(MyInfoUpdateRequestStatus.PENDING))
                        .exists();

        return hasPendingMyInfoRequest ? existsPendingRequest : existsPendingRequest.not();
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

    private BooleanExpression excludeMasterAdmin() {
        return user.role.ne(Role.MASTER_ADMIN);
    }

    private BooleanExpression workLocationFilter(Company workLocation) {
        return workLocation != null ? user.workLocation.eq(workLocation) : null;
    }
}
