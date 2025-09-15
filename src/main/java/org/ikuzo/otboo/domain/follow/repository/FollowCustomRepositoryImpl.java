package org.ikuzo.otboo.domain.follow.repository;

import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ikuzo.otboo.domain.follow.entity.Follow;
import org.ikuzo.otboo.domain.follow.entity.QFollow;
import org.ikuzo.otboo.domain.user.entity.QUser;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
@Slf4j
public class FollowCustomRepositoryImpl implements FollowCustomRepository {

    private final JPAQueryFactory jpaQueryFactory;

    @Override
    public List<Follow> getFollows(UUID followeeId, String cursor, UUID idAfter, int limit, String nameLike, String type) {
        QFollow follow = QFollow.follow;
        QUser follower = new QUser("follower");
        QUser following = new QUser("following");

        JPAQuery<Follow> query = jpaQueryFactory.selectFrom(follow);

        if (type.equals("follower")) {
            query.join(follow.follower, follower).fetchJoin()
                .join(follow.following, following).fetchJoin()
                .where(follow.following.id.eq(followeeId));

            if (nameLike != null && !nameLike.isBlank()) {
                query.where(
                    Expressions.booleanTemplate("{0} ILIKE {1}", follower.name, "%" + nameLike.trim() + "%")
                );
            }
        } else if (type.equals("following")) {
            query.join(follow.follower, follower).fetchJoin()
                .join(follow.following, following).fetchJoin()
                .where(follow.follower.id.eq(followeeId));

            if (nameLike != null && !nameLike.isBlank()) {
                query.where(
                    Expressions.booleanTemplate("{0} ILIKE {1}", following.name, "%" + nameLike.trim() + "%")
                );
            }
        }

        if (cursor != null && !cursor.isBlank() && idAfter != null) {
            Instant cursorInstant = Instant.parse(cursor);

            query.where(
                follow.createdAt.lt(cursorInstant)
                    .or(follow.createdAt.eq(cursorInstant)
                        .and(follow.id.lt(idAfter)))
            );
        }

        return query
            .orderBy(follow.createdAt.desc())
            .limit(limit + 1)
            .fetch();
    }

    @Override
    public long countByCursorFilter(UUID followeeId, String nameLike, String type) {
        QFollow follow = QFollow.follow;
        QUser follower = new QUser("follower");
        QUser following = new QUser("following");

        JPAQuery<Long> query = jpaQueryFactory.select(follow.count()).from(follow);

        if (type.equals("follower")) {
            query.join(follow.follower, follower)
                .where(follow.following.id.eq(followeeId));

            if (nameLike != null && !nameLike.isBlank()) {
                query.where(
                    Expressions.booleanTemplate("{0} ILIKE {1}", follower.name, "%" + nameLike.trim() + "%")
                );
            }
        } else if (type.equals("following")) {
            query.join(follow.following, following)
                .where(follow.follower.id.eq(followeeId));

            if (nameLike != null && !nameLike.isBlank()) {
                query.where(
                    Expressions.booleanTemplate("{0} ILIKE {1}", following.name, "%" + nameLike.trim() + "%")
                );
            }
        }


        return query.fetchFirst();
    }
}
