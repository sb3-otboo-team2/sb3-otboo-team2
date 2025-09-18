package org.ikuzo.otboo.domain.clothes.controller;

import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ikuzo.otboo.domain.clothes.controller.api.ClothesAttributeDefApi;
import org.ikuzo.otboo.domain.clothes.dto.ClothesAttributeDefDto;
import org.ikuzo.otboo.domain.clothes.dto.request.ClothesAttributeDefCreateRequest;
import org.ikuzo.otboo.domain.clothes.dto.request.ClothesAttributeDefUpdateRequest;
import org.ikuzo.otboo.domain.clothes.service.ClothesAttributeDefService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/clothes/attribute-defs")
public class ClothesAttributeDefController implements ClothesAttributeDefApi {

    private final ClothesAttributeDefService clothesAttributeDefService;

    @PostMapping
    @Override
    public ResponseEntity<ClothesAttributeDefDto> create(
        @RequestBody @Valid ClothesAttributeDefCreateRequest request
    ) {
        log.info("[Controller] 속성 등록 요청 - name: {}", request.name());

        ClothesAttributeDefDto dto = clothesAttributeDefService.create(request);

        log.info("[Controller] 속성 등록 완료 - name: {}", dto.name());

        return ResponseEntity.status(HttpStatus.CREATED).body(dto);
    }

    @PatchMapping("/{definitionId}")
    @Override
    public ResponseEntity<ClothesAttributeDefDto> update(
        @PathVariable UUID definitionId,
        @RequestBody @Valid ClothesAttributeDefUpdateRequest request
    ) {
        log.info("[Controller] 속성 수정 요청 - name: {}", request.name());

        ClothesAttributeDefDto dto = clothesAttributeDefService.update(definitionId, request);

        log.info("[Controller] 속성 수정 완료 - name: {}", dto.name());

        return ResponseEntity.status(HttpStatus.OK).body(dto);
    }

    @DeleteMapping("/{definitionId}")
    @Override
    public ResponseEntity<Void> delete(
        @PathVariable UUID definitionId
    ) {
        log.info("[Controller] 속성 삭제 요청 - definitionId: {}", definitionId);

        clothesAttributeDefService.delete(definitionId);

        log.info("[Controller] 속성 삭제 완료 - definitionId: {}", definitionId);

        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }
}
