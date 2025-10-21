package org.ikuzo.otboo.domain.feed.repository;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ikuzo.otboo.domain.clothes.entity.QClothes;
import org.ikuzo.otboo.domain.feed.entity.Feed;
import org.ikuzo.otboo.domain.feed.entity.QFeed;
import org.ikuzo.otboo.domain.feed.entity.QFeedClothes;
import org.ikuzo.otboo.domain.feed.repository.dto.FeedSortKey;
import org.ikuzo.otboo.domain.user.entity.QUser;
import org.ikuzo.otboo.domain.weather.entity.QWeather;
import org.springframework.stereotype.Repository;

@Slf4j
@Repository
@RequiredArgsConstructor
public class FeedCustomRepositoryImpl implements FeedCustomRepository {

    private final JPAQueryFactory queryFactory;

    @Override
    public List<Feed> findFeedsWithCursor(String cursor,
                                          UUID idAfter,
                                          int limit,
                                          FeedSortKey sortKey,
                                          boolean ascending,
                                          String keywordLike,
                                          String skyStatusEqual,
                                          String precipitationTypeEqual,
                                          UUID authorIdEqual) {
        QFeed feed = QFeed.feed;
        QWeather weather = QWeather.weather;
        QUser author = QUser.user;
        QFeedClothes feedClothes = QFeedClothes.feedClothes;
        QClothes clothes = QClothes.clothes;

        BooleanBuilder filter = buildBaseFilter(keywordLike, skyStatusEqual, precipitationTypeEqual, authorIdEqual,
            feed, weather, author);

        BooleanExpression cursorPredicate = buildCursorPredicate(sortKey, ascending, cursor, idAfter, feed);
        if (cursorPredicate != null) {
            filter.and(cursorPredicate);
        }

        // 1차 쿼리: ID만 조회하여 페이징 안정성 확보
        JPAQuery<UUID> idQuery = queryFactory.select(feed.id)
            .from(feed)
            .leftJoin(feed.weather, weather)
            .leftJoin(feed.author, author)
            .where(filter)
            .limit(limit + 1L);

        applyOrder(idQuery, sortKey, ascending, feed);

        List<UUID> feedIds = idQuery.fetch();
        if (feedIds.isEmpty()) {
            return List.of();
        }

        // 2차 쿼리: 필요한 연관 데이터를 fetch join으로 로딩
        JPAQuery<Feed> query = queryFactory.selectDistinct(feed)
            .from(feed)
            .leftJoin(feed.weather, weather).fetchJoin()
            .leftJoin(feed.author, author).fetchJoin()
            .leftJoin(feed.feedClothes, feedClothes).fetchJoin()
            .leftJoin(feedClothes.clothes, clothes).fetchJoin()
            .where(feed.id.in(feedIds));

        List<Feed> rows = query.fetch();

        Map<UUID, Feed> feedsById = new HashMap<>();
        for (Feed entity : rows) {
            feedsById.put(entity.getId(), entity);
        }

        List<Feed> orderedFeeds = new ArrayList<>(feedIds.size());
        for (UUID feedId : feedIds) {
            Feed entity = feedsById.get(feedId);
            if (entity != null) {
                orderedFeeds.add(entity);
            }
        }

        return orderedFeeds;
    }

    @Override
    public long countFeeds(String keywordLike,
                           String skyStatusEqual,
                           String precipitationTypeEqual,
                           UUID authorIdEqual) {
        QFeed feed = QFeed.feed;
        QWeather weather = QWeather.weather;
        QUser author = QUser.user;

        BooleanBuilder filter = buildBaseFilter(keywordLike, skyStatusEqual, precipitationTypeEqual, authorIdEqual,
            feed, weather, author);

        Long total = queryFactory.select(feed.id.countDistinct())
            .from(feed)
            .leftJoin(feed.weather, weather)
            .leftJoin(feed.author, author)
            .where(filter)
            .fetchOne();

        return total != null ? total : 0L;
    }

    private BooleanBuilder buildBaseFilter(String keywordLike,
                                           String skyStatusEqual,
                                           String precipitationTypeEqual,
                                           UUID authorIdEqual,
                                           QFeed feed,
                                           QWeather weather,
                                           QUser author) {
        BooleanBuilder builder = new BooleanBuilder();

        if (keywordLike != null && !keywordLike.isBlank()) {
            builder.and(
                Expressions.booleanTemplate(
                    "{0} ILIKE {1}",
                    feed.content,
                    "%" + keywordLike.trim() + "%"
                )
            );
        }

        if (skyStatusEqual != null && !skyStatusEqual.isBlank()) {
            builder.and(weather.skyStatus.eq(skyStatusEqual.trim().toUpperCase()));
        }

        if (precipitationTypeEqual != null && !precipitationTypeEqual.isBlank()) {
            builder.and(weather.precipitationType.eq(precipitationTypeEqual.trim().toUpperCase()));
        }

        if (authorIdEqual != null) {
            builder.and(author.id.eq(authorIdEqual));
        }

        return builder;
    }

    private BooleanExpression buildCursorPredicate(FeedSortKey sortKey,
                                                   boolean ascending,
                                                   String cursor,
                                                   UUID idAfter,
                                                   QFeed feed) {
        if (cursor == null || cursor.isBlank()) {
            return null;
        }

        if (sortKey == FeedSortKey.CREATED_AT) {
            Instant cursorInstant = parseInstant(cursor);
            if (cursorInstant == null) {
                return null;
            }
            BooleanExpression primary = ascending ? feed.createdAt.gt(cursorInstant) : feed.createdAt.lt(cursorInstant);
            BooleanExpression secondary = feed.createdAt.eq(cursorInstant);
            if (idAfter != null) {
                secondary = secondary.and(ascending ? feed.id.gt(idAfter) : feed.id.lt(idAfter));
                return primary.or(secondary);
            }
            return primary;
        }

        Long cursorValue = parseLong(cursor);
        if (cursorValue == null) {
            return null;
        }
        BooleanExpression primary = ascending ? feed.likeCount.gt(cursorValue) : feed.likeCount.lt(cursorValue);
        BooleanExpression secondary = feed.likeCount.eq(cursorValue);
        if (idAfter != null) {
            secondary = secondary.and(ascending ? feed.id.gt(idAfter) : feed.id.lt(idAfter));
            return primary.or(secondary);
        }
        return primary;
    }

    private void applyOrder(JPAQuery<?> query, FeedSortKey sortKey, boolean ascending, QFeed feed) {
        if (sortKey == FeedSortKey.CREATED_AT) {
            OrderSpecifier<Instant> primary = ascending ? feed.createdAt.asc() : feed.createdAt.desc();
            OrderSpecifier<UUID> secondary = ascending ? feed.id.asc() : feed.id.desc();
            query.orderBy(primary, secondary);
            return;
        }
        OrderSpecifier<Long> primary = ascending ? feed.likeCount.asc() : feed.likeCount.desc();
        OrderSpecifier<UUID> secondary = ascending ? feed.id.asc() : feed.id.desc();
        query.orderBy(primary, secondary);
    }

    private Instant parseInstant(String value) {
        try {
            return Instant.parse(value);
        } catch (DateTimeParseException | NullPointerException e) {
            log.warn("[FeedRepository] createdAt 커서 값이 올바르지 않습니다: {}", value, e);
            return null;
        }
    }

    private Long parseLong(String value) {
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException | NullPointerException e) {
            log.warn("[FeedRepository] likeCount 커서 값이 올바르지 않습니다: {}", value, e);
            return null;
        }
    }
}
