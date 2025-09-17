package org.ikuzo.otboo.domain.clothes.service;

import java.util.UUID;
import org.ikuzo.otboo.domain.clothes.dto.request.ClothesAttributeDefCreateRequest;
import org.ikuzo.otboo.domain.clothes.dto.ClothesAttributeDefDto;
import org.ikuzo.otboo.domain.clothes.dto.request.ClothesAttributeDefUpdateRequest;

public interface ClothesAttributeDefService {

    ClothesAttributeDefDto create(ClothesAttributeDefCreateRequest request);

    ClothesAttributeDefDto update(UUID definitionId, ClothesAttributeDefUpdateRequest request);

}
