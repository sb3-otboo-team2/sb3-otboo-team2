package org.ikuzo.otboo.domain.follow.repository;

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
    public List<Follow> getFollowers(UUID followeeId, String cursor, UUID idAfter, int limit, String nameLike) {
        QFollow follow = QFollow.follow;
        QUser follower = QUser.user;
        QUser following = new QUser("following");

        JPAQuery<Follow> query = jpaQueryFactory
            .selectFrom(follow)
            .join(follow.follower, follower).fetchJoin()
            .join(follow.following, following).fetchJoin()
            .where(follow.following.id.eq(followeeId));

        if (cursor != null && !cursor.isBlank() && idAfter != null) {
            Instant cursorInstant = Instant.parse(cursor);

            query.where(
                follow.createdAt.lt(cursorInstant)
                    .or(follow.createdAt.eq(cursorInstant)
                        .and(follow.id.lt(idAfter)))
            );
        }

        // 이름 검색
        if (nameLike != null && !nameLike.isBlank()) {
            query.where(follower.name.containsIgnoreCase(nameLike));
        }

        // 정렬 + limit
        return query
            .orderBy(follow.createdAt.desc())
            .limit(limit + 1)
            .fetch();
    }

    @Override
    public long countByCursorFilter(UUID followeeId, String cursor, UUID idAfter, int limit, String nameLike) {
        QFollow follow = QFollow.follow;
        QUser follower = QUser.user;

        JPAQuery<Long> query = jpaQueryFactory
            .select(follow.count())
            .from(follow)
            .join(follow.follower, follower)
            .where(follow.following.id.eq(followeeId));

        // 이름 검색
        if (nameLike != null && !nameLike.isBlank()) {
            query.where(follower.name.containsIgnoreCase(nameLike));
        }

        return query.fetchFirst();
    }

    @Override
    public List<Follow> getFollowings(UUID followeeId, String cursor, UUID idAfter, int limit, String nameLike) {
        QFollow follow = QFollow.follow;
        QUser follower = new QUser("follower");
        QUser following = new QUser("following");

        JPAQuery<Follow> query = jpaQueryFactory
            .selectFrom(follow)
            .join(follow.follower, follower).fetchJoin()
            .join(follow.following, following).fetchJoin()
            .where(follow.follower.id.eq(followeeId));

        if (cursor != null && !cursor.isBlank() && idAfter != null) {
            Instant cursorInstant = Instant.parse(cursor);

            query.where(
                follow.createdAt.lt(cursorInstant)
                    .or(follow.createdAt.eq(cursorInstant)
                        .and(follow.id.lt(idAfter)))
            );
        }

        // 이름 검색
        if (nameLike != null && !nameLike.isBlank()) {
            query.where(following.name.containsIgnoreCase(nameLike));
        }

        return query
            .orderBy(follow.createdAt.desc())
            .limit(limit + 1)
            .fetch();
    }
}
