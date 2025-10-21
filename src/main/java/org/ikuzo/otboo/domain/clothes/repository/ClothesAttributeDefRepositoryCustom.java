package org.ikuzo.otboo.domain.clothes.repository;

import java.util.List;
import org.ikuzo.otboo.domain.clothes.entity.ClothesAttributeDef;
import org.ikuzo.otboo.domain.clothes.enums.AttributeDefSortBy;
import org.ikuzo.otboo.domain.clothes.enums.AttributeDefSortDirection;

public interface ClothesAttributeDefRepositoryCustom {

    List<ClothesAttributeDef> findAttributeDefWithCursor (
        AttributeDefSortBy sortBy,
        AttributeDefSortDirection direction,
        String keywordLike
    );
}
