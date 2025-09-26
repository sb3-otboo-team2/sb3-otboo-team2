package org.ikuzo.otboo.domain.comment.repository;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.ikuzo.otboo.domain.comment.entity.Comment;
import org.ikuzo.otboo.domain.comment.entity.QComment;
import org.ikuzo.otboo.domain.user.entity.QUser;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class CommentRepositoryImpl implements CommentCustomRepository {

    private final JPAQueryFactory queryFactory;

    @Override
    public List<Comment> findCommentsWithCursor(UUID feedId, Instant cursor, UUID idAfter, int limit) {
        QComment comment = QComment.comment;
        QUser author = QUser.user;

        BooleanBuilder builder = new BooleanBuilder();
        builder.and(comment.feed.id.eq(feedId));

        BooleanExpression cursorPredicate = buildCursorPredicate(comment, cursor, idAfter);
        if (cursorPredicate != null) {
            builder.and(cursorPredicate);
        }

        JPAQuery<Comment> query = queryFactory.selectFrom(comment)
            .join(comment.author, author).fetchJoin()
            .where(builder)
            .orderBy(comment.createdAt.desc(), comment.id.desc())
            .limit(limit + 1L);

        return query.fetch();
    }

    private BooleanExpression buildCursorPredicate(QComment comment, Instant cursor, UUID idAfter) {
        if (cursor == null) {
            return null;
        }

        BooleanExpression primary = comment.createdAt.lt(cursor);
        BooleanExpression secondary = comment.createdAt.eq(cursor);
        if (idAfter != null) {
            secondary = secondary.and(comment.id.lt(idAfter));
            return primary.or(secondary);
        }
        return primary;
    }
}