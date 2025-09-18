package org.ikuzo.otboo.domain.clothes.service;

import java.util.List;
import java.util.UUID;
import org.ikuzo.otboo.domain.clothes.dto.request.ClothesAttributeDefCreateRequest;
import org.ikuzo.otboo.domain.clothes.dto.ClothesAttributeDefDto;
import org.ikuzo.otboo.domain.clothes.dto.request.ClothesAttributeDefUpdateRequest;
import org.ikuzo.otboo.domain.clothes.enums.AttributeDefSortBy;
import org.ikuzo.otboo.domain.clothes.enums.AttributeDefSortDirection;

public interface ClothesAttributeDefService {

    ClothesAttributeDefDto create(ClothesAttributeDefCreateRequest request);

    ClothesAttributeDefDto update(UUID definitionId, ClothesAttributeDefUpdateRequest request);

    void delete(UUID definitionId);

    List<ClothesAttributeDefDto> getWithCursor(
        AttributeDefSortBy sortBy,
        AttributeDefSortDirection sortDirection,
        String keywordLike
    );
}
