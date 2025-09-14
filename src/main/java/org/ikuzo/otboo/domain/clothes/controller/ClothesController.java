package org.ikuzo.otboo.domain.clothes.controller;

import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ikuzo.otboo.domain.clothes.controller.api.ClothesApi;
import org.ikuzo.otboo.domain.clothes.dto.ClothesDto;
import org.ikuzo.otboo.domain.clothes.dto.request.ClothesCreateRequest;
import org.ikuzo.otboo.domain.clothes.dto.request.ClothesUpdateRequest;
import org.ikuzo.otboo.domain.clothes.service.ClothesService;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/clothes")
public class ClothesController implements ClothesApi {

    private final ClothesService clothesService;

    @PostMapping(
        consumes = MediaType.MULTIPART_FORM_DATA_VALUE,
        produces = MediaType.APPLICATION_JSON_VALUE
    )
    @Override
    public ResponseEntity<ClothesDto> create(
        @RequestPart("request") @Valid ClothesCreateRequest request,
        @RequestPart(value = "image", required = false) MultipartFile image) {

        log.info("[Controller] 의상 등록 요청 - ownerId: {}, name: {}",
            request.ownerId(), request.name());

        ClothesDto response = clothesService.create(request, image);

        log.info("[Controller] 의상 등록 완료 - ownerId: {}, name: {}",
            response.ownerId(), response.name());

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PatchMapping(
        path = "/{clothesId}",
        consumes = MediaType.MULTIPART_FORM_DATA_VALUE,
        produces = MediaType.APPLICATION_JSON_VALUE
    )
    @Override
    public ResponseEntity<ClothesDto> update(
        @PathVariable("clothesId") UUID clothesId,
        @RequestPart("request") @Valid ClothesUpdateRequest request,
        @RequestPart(value = "image", required = false) MultipartFile image
    ) {
        log.info("[Controller] 의상 수정 요청 - name: {}", request.name());

        ClothesDto response = clothesService.update(clothesId, request, image);

        log.info("[Controller] 의상 수정 완료 - name: {}", response.name());

        return ResponseEntity.status(HttpStatus.OK).body(response);
    }
}
