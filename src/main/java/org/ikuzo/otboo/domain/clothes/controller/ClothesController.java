package org.ikuzo.otboo.domain.clothes.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ikuzo.otboo.domain.clothes.controller.api.ClothesApi;
import org.ikuzo.otboo.domain.clothes.dto.ClothesDto;
import org.ikuzo.otboo.domain.clothes.dto.request.ClothesCreateRequest;
import org.ikuzo.otboo.domain.clothes.dto.request.ClothesUpdateRequest;
import org.ikuzo.otboo.domain.clothes.service.ClothesService;
import org.ikuzo.otboo.domain.clothes.service.ClothingExtractionService;
import org.ikuzo.otboo.global.dto.PageResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import reactor.core.publisher.Mono;

@Slf4j
@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/clothes")
public class ClothesController implements ClothesApi {

    private final ClothesService clothesService;
    private final ClothingExtractionService clothingExtractionService;

    @GetMapping
    @Override
    public ResponseEntity<PageResponse<ClothesDto>> getWithCursor(
        @RequestParam UUID ownerId,
        @RequestParam(required = false) String cursor,
        @RequestParam(required = false) UUID idAfter,
        @RequestParam(defaultValue = "20") @Min(1) @Max(100) int limit,
        @RequestParam(required = false) String typeEqual
    ) {
        log.info("[Controller] 의상 목록 조회 요청 - ownerId: {}", ownerId);

        PageResponse<ClothesDto> response =
            clothesService.getWithCursor(ownerId, cursor, idAfter, limit, typeEqual);

        log.info("[Controller] 의상 목록 조회 완료 - ownerId: {}", ownerId);

        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    @PostMapping(
        consumes = MediaType.MULTIPART_FORM_DATA_VALUE,
        produces = MediaType.APPLICATION_JSON_VALUE
    )
    @Override
    public ResponseEntity<ClothesDto> create(
        @RequestPart("request") @Valid ClothesCreateRequest request,
        @RequestPart(value = "image", required = false) MultipartFile image
    ) {

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

    @DeleteMapping("/{clothesId}")
    @Override
    public ResponseEntity<Void> delete(
        @PathVariable("clothesId") UUID clothesId
    ) {
        log.info("[Controller] 의상 삭제 요청 - clothesId: {}", clothesId);

        clothesService.delete(clothesId);

        log.info("[Controller] 의상 삭제 완료 - clothesId: {}", clothesId);

        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }

    @GetMapping(
        value = "/extractions",
        produces = MediaType.APPLICATION_JSON_VALUE
    )
    @Override
    public Mono<ResponseEntity<ClothesDto>> extractByUrl(
        @RequestParam("url") String url
    ){
        return clothingExtractionService.extractFromUrlReactive(url)
            .map(ResponseEntity::ok)
            .onErrorResume(e -> {
                log.error("[Extraction] failed: {}", e.getMessage(), e);
                return Mono.just(ResponseEntity.status(HttpStatus.BAD_REQUEST).build());
            });
    }
}
