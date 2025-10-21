package org.ikuzo.otboo.domain.clothes.dto;

import java.util.List;
import java.util.UUID;
import org.ikuzo.otboo.domain.clothes.enums.ClothesType;

public record ClothesDto(
    UUID id,
    UUID ownerId,
    String name,
    String imageUrl,
    ClothesType type,
    List<ClothesAttributeWithDefDto> attributes
) {

}
