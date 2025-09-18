package org.ikuzo.otboo.domain.clothes.repository.impl;

import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ikuzo.otboo.domain.clothes.entity.ClothesAttributeDef;
import org.ikuzo.otboo.domain.clothes.entity.QClothesAttributeDef;
import org.ikuzo.otboo.domain.clothes.enums.AttributeDefSortBy;
import org.ikuzo.otboo.domain.clothes.enums.AttributeDefSortDirection;
import org.ikuzo.otboo.domain.clothes.repository.ClothesAttributeDefRepositoryCustom;
import org.springframework.stereotype.Repository;

@Slf4j
@Repository
@RequiredArgsConstructor
public class ClothesAttributeDefRepositoryImpl implements ClothesAttributeDefRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    @Override
    public List<ClothesAttributeDef> findAttributeDefWithCursor(
        String cursor,
        UUID idAfter,
        int limit,
        AttributeDefSortBy sortBy,
        AttributeDefSortDirection direction,
        String keywordLike
    ) {
        QClothesAttributeDef d = QClothesAttributeDef.clothesAttributeDef;

        boolean asc = (direction == AttributeDefSortDirection.ASCENDING);

        return queryFactory
            .selectFrom(d)
            .where(
                likeKeyword(d, keywordLike),
                cursorCondition(d, cursor, idAfter, sortBy, asc)
            )
            .orderBy(orderBy(d, sortBy, asc))
            .limit(limit + 1)
            .fetch();
    }

    private BooleanExpression likeKeyword(
        QClothesAttributeDef d, String keywordLike
    ) {
        if (keywordLike == null || keywordLike.isBlank()) {
            return null;
        }
        return d.name.containsIgnoreCase(keywordLike.trim());
    }

    private BooleanExpression cursorCondition(
        QClothesAttributeDef d,
        String cursor,
        UUID idAfter,
        AttributeDefSortBy sortBy,
        boolean asc
    ) {
        return switch (sortBy) {
            case createdAt -> cursorByCreatedAt(d, cursor, idAfter, asc);
            case name -> cursorByName(d, cursor, idAfter, asc);
        };
    }

    private BooleanExpression cursorByCreatedAt(
        QClothesAttributeDef d, String cursor, UUID idAfter, boolean asc
    ) {
        if (cursor == null || cursor.isBlank()) {
            return null;
        }

        final Instant c;
        try {
            c = Instant.parse(cursor.trim());
        } catch (DateTimeParseException e) {
            log.warn("잘못된 createdAt 커서 포맷: {}", cursor);
            return null;
        }

        BooleanExpression primary = asc ? d.createdAt.gt(c) : d.createdAt.lt(c);
        if (idAfter == null) {
            return primary;
        }

        BooleanExpression tie = asc ? d.id.gt(idAfter) : d.id.lt(idAfter);

        return primary.or(d.createdAt.eq(c).and(tie));
    }

    private BooleanExpression cursorByName(
        QClothesAttributeDef d, String cursor, UUID idAfter, boolean asc
    ) {
        if (cursor == null || cursor.isBlank()) {
            return null;
        }

        String c = cursor.trim();
        BooleanExpression primary = asc ? d.name.gt(c) : d.name.lt(c);
        if (idAfter == null) {
            return primary;
        }

        BooleanExpression tie = asc ? d.id.gt(idAfter) : d.id.lt(idAfter);

        return primary.or(d.name.eq(c).and(tie));
    }

    private OrderSpecifier<?>[] orderBy(
        QClothesAttributeDef d, AttributeDefSortBy sortBy, boolean asc
    ) {
        OrderSpecifier<?> primary = (sortBy == AttributeDefSortBy.createdAt)
            ? (asc ? d.createdAt.asc() : d.createdAt.desc())
            : (asc ? d.name.asc() : d.name.desc());
        OrderSpecifier<?> tie = asc ? d.id.asc() : d.id.desc();

        return new OrderSpecifier<?>[]{primary, tie};
    }
}
