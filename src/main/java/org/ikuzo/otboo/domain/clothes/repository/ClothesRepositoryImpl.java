package org.ikuzo.otboo.domain.clothes.repository;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ikuzo.otboo.domain.clothes.entity.Clothes;
import org.ikuzo.otboo.domain.clothes.entity.ClothesType;
import org.ikuzo.otboo.domain.clothes.entity.QClothes;
import org.springframework.stereotype.Repository;

@Slf4j
@Repository
@RequiredArgsConstructor
public class ClothesRepositoryImpl implements ClothesRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    @Override
    public List<Clothes> findClothesWithCursor(
        UUID ownerId,
        String cursor,
        UUID idAfter,
        int limit,
        String typeEqual
    ) {
        QClothes c = QClothes.clothes;

        BooleanBuilder where = new BooleanBuilder()
            .and(c.owner.id.eq(ownerId))
            .and(eqType(c, typeEqual))
            .and(cursorCondition(c, cursor, idAfter));

        OrderSpecifier<?> orderByCreatedDesc = c.createdAt.desc();
        OrderSpecifier<?> orderByIdDesc = c.id.desc();

        return queryFactory
            .selectFrom(c)
            .where(where)
            .orderBy(orderByCreatedDesc, orderByIdDesc)
            .limit(limit + 1)
            .fetch();
    }

    @Override
    public Long countClothes(UUID ownerId, String typeEqual) {
        QClothes c = QClothes.clothes;

        Long count = queryFactory
            .select(c.count())
            .from(c)
            .where(
                c.owner.id.eq(ownerId),
                eqType(c, typeEqual)
            )
            .fetchOne();

        return count != null ? count : 0L;
    }

    private BooleanExpression eqType(QClothes clothes, String typeEqual) {
        if (typeEqual == null || typeEqual.isBlank()) {
            return null;
        }
        try {
            ClothesType type = ClothesType.valueOf(typeEqual);
            return clothes.type.eq(type);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("type is invalid " + typeEqual);
        }
    }

    private BooleanExpression cursorCondition(QClothes clothes, String cursor, UUID idAfter) {
        if (cursor == null || cursor.isBlank()) {
            return null;
        }

        try {
            Instant createdAt = Instant.parse(cursor);

            if (idAfter == null) {
                return clothes.createdAt.lt(createdAt);
            }
            return clothes.createdAt.lt(createdAt)
                .or(clothes.createdAt.eq(createdAt).and(clothes.id.lt(idAfter)));
        } catch (DateTimeParseException e) {
            log.warn("잘못된 커서 포맷: {}", cursor);
            return null;
        }
    }
}
