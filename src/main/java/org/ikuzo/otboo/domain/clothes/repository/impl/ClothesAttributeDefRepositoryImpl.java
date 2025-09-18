package org.ikuzo.otboo.domain.clothes.repository.impl;

import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import java.util.List;
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
        AttributeDefSortBy sortBy,
        AttributeDefSortDirection direction,
        String keywordLike
    ) {
        QClothesAttributeDef d = QClothesAttributeDef.clothesAttributeDef;

        boolean asc = (direction == AttributeDefSortDirection.ASCENDING);

        return queryFactory
            .selectFrom(d)
            .where(
                likeKeyword(d, keywordLike)
            )
            .orderBy(orderBy(d, sortBy, asc))
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
