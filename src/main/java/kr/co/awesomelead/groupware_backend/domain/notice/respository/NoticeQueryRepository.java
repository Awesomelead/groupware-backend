package kr.co.awesomelead.groupware_backend.domain.notice.respository;

import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import java.util.List;
import kr.co.awesomelead.groupware_backend.domain.notice.dto.request.NoticeSearchConditionDto;
import kr.co.awesomelead.groupware_backend.domain.notice.dto.response.NoticeSummaryDto;
import kr.co.awesomelead.groupware_backend.domain.notice.entity.QNotice;
import kr.co.awesomelead.groupware_backend.domain.notice.entity.QNoticeTarget;
import kr.co.awesomelead.groupware_backend.domain.notice.enums.NoticeSearchType;
import kr.co.awesomelead.groupware_backend.domain.notice.enums.NoticeType;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.support.PageableExecutionUtils;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

@Repository
@RequiredArgsConstructor
public class NoticeQueryRepository {

    private final JPAQueryFactory queryFactory;

    public Page<NoticeSummaryDto> findNoticesWithFilters(
        NoticeSearchConditionDto conditionDto,
        Long userId,
        boolean hasAccessNotice,
        Pageable pageable) {

        QNotice notice = QNotice.notice;
        QNoticeTarget noticeTarget = QNoticeTarget.noticeTarget;

        var query = queryFactory
            .from(notice);

        if (!hasAccessNotice) {
            query.innerJoin(noticeTarget).on(noticeTarget.notice.eq(notice))
                .where(noticeTarget.user.id.eq(userId));
        }

        query.where(
            typeEq(conditionDto.getType()),
            searchKeyword(conditionDto.getKeyword(), conditionDto.getSearchType())
        );

        List<NoticeSummaryDto> result = query
            .select(Projections.constructor(NoticeSummaryDto.class,
                notice.id,
                notice.type,
                notice.title,
                notice.pinned,
                notice.updatedDate
            ))
            .orderBy(notice.pinned.desc(), notice.updatedDate.desc())
            .offset(pageable.getOffset())
            .limit(pageable.getPageSize())
            .fetch();
        return PageableExecutionUtils.getPage(
            result,
            pageable,
            () -> {
                Long total = queryFactory
                    .select(notice.count())
                    .from(notice)
                    .where(
                        noticeAccessible(hasAccessNotice, userId),
                        typeEq(conditionDto.getType()),
                        searchKeyword(conditionDto.getKeyword(), conditionDto.getSearchType())
                    )
                    .fetchOne();
                return total != null ? total : 0L;
            }
        );
    }

    public List<NoticeSummaryDto> findTop3Notices(Long userId,
        boolean hasAccessNotice) {
        QNotice notice = QNotice.notice;

        return queryFactory
            .select(Projections.constructor(NoticeSummaryDto.class,
                notice.id,
                notice.type,
                notice.title,
                notice.pinned,
                notice.updatedDate
            ))
            .from(notice)
            .where(
                noticeAccessible(hasAccessNotice, userId)
            )
            .orderBy(notice.pinned.desc(), notice.updatedDate.desc())
            .limit(3)
            .fetch();
    }

    private BooleanExpression noticeAccessible(boolean hasAccessNotice, Long userId) {
        if (hasAccessNotice) {
            return null; // 관리자는 필터링 없음
        }

        // 내 ID가 타겟 테이블에 존재하는지 확인하는 서브쿼리
        // (Join 방식이 아닌 exists 방식이 필요한 곳을 위해 남겨둠)
        return QNotice.notice.id.in(
            JPAExpressions.select(QNoticeTarget.noticeTarget.notice.id)
                .from(QNoticeTarget.noticeTarget)
                .where(QNoticeTarget.noticeTarget.user.id.eq(userId))
        );
    }

    private BooleanExpression typeEq(NoticeType type) {
        return type != null ? QNotice.notice.type.eq(type) : null;
    }

    private BooleanExpression searchKeyword(String keyword, NoticeSearchType searchType) {
        if (!StringUtils.hasText(keyword)) {
            return null;
        }
        QNotice n = QNotice.notice;

        NoticeSearchType type = (searchType != null) ? searchType : NoticeSearchType.ALL;

        return switch (type) {
            case TITLE -> n.title.contains(keyword);
            case CONTENT -> n.content.contains(keyword);
            case AUTHOR -> n.author.nameKor.contains(keyword);
            default -> n.title.contains(keyword).or(n.content.contains(keyword));
        };
    }


}
