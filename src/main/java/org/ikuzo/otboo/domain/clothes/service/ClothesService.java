package org.ikuzo.otboo.domain.clothes.service;

import java.util.UUID;
import org.ikuzo.otboo.domain.clothes.dto.ClothesDto;
import org.ikuzo.otboo.domain.clothes.dto.request.ClothesCreateRequest;
import org.ikuzo.otboo.domain.clothes.dto.request.ClothesUpdateRequest;
import org.ikuzo.otboo.global.dto.PageResponse;
import org.springframework.web.multipart.MultipartFile;

public interface ClothesService {

    ClothesDto create(ClothesCreateRequest request, MultipartFile image);

    ClothesDto update(UUID clothesId, ClothesUpdateRequest request, MultipartFile image);

    void delete(UUID clothesId);

    PageResponse<ClothesDto> getWithCursor(
        UUID ownerId, String cursor, UUID idAfter, int limit, String typeEqual
    );

}
