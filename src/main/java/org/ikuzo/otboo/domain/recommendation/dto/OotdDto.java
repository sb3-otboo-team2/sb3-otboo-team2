package org.ikuzo.otboo.domain.recommendation.dto;

import java.util.List;
import java.util.UUID;
import lombok.Builder;
import org.ikuzo.otboo.domain.clothes.dto.ClothesAttributeWithDefDto;
import org.ikuzo.otboo.domain.clothes.enums.ClothesType;

@Builder
public record OotdDto(
    UUID clothesId,
    String name,
    String imageUrl,
    ClothesType type,
    List<ClothesAttributeWithDefDto> attributes
) {

}