package org.ikuzo.otboo.domain.clothes.service;

import org.ikuzo.otboo.domain.clothes.dto.ClothesDto;
import org.ikuzo.otboo.domain.clothes.dto.request.ClothesCreateRequest;
import org.springframework.web.multipart.MultipartFile;

public interface ClothesService {

    ClothesDto create(ClothesCreateRequest request, MultipartFile image);

}
