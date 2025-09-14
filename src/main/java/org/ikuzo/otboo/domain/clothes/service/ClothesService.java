package org.ikuzo.otboo.domain.clothes.service;

import java.util.UUID;
import org.ikuzo.otboo.domain.clothes.dto.ClothesDto;
import org.ikuzo.otboo.domain.clothes.dto.request.ClothesCreateRequest;
import org.ikuzo.otboo.domain.clothes.dto.request.ClothesUpdateRequest;
import org.springframework.web.multipart.MultipartFile;

public interface ClothesService {

    ClothesDto create(ClothesCreateRequest request, MultipartFile image);

    ClothesDto update(UUID clothesId, ClothesUpdateRequest request, MultipartFile image);

}
