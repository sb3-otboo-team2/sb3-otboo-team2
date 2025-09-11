package org.ikuzo.otboo.domain.clothes.controller;

import lombok.RequiredArgsConstructor;
import org.ikuzo.otboo.domain.clothes.dto.ClothesAttributeDefDto;
import org.ikuzo.otboo.domain.clothes.dto.request.ClothesAttributeDefCreateRequest;
import org.ikuzo.otboo.domain.clothes.service.ClothesAttributeDefService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/clothes/attribute-defs")
public class ClothesAttributeDefController {

    private final ClothesAttributeDefService clothesAttributeDefService;

    //    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping
    public ResponseEntity<ClothesAttributeDefDto> create(
        @RequestBody ClothesAttributeDefCreateRequest request
    ) {
        ClothesAttributeDefDto dto = clothesAttributeDefService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(dto);
    }
}
