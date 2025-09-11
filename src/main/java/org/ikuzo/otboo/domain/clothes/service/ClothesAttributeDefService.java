package org.ikuzo.otboo.domain.clothes.service;

import org.ikuzo.otboo.domain.clothes.dto.request.ClothesAttributeDefCreateRequest;
import org.ikuzo.otboo.domain.clothes.dto.ClothesAttributeDefDto;

public interface ClothesAttributeDefService {

    ClothesAttributeDefDto create(ClothesAttributeDefCreateRequest request);

}
