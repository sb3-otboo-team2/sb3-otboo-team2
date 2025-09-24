package org.ikuzo.otboo.domain.recommendation.dto;

import java.util.List;
import java.util.UUID;
import lombok.Builder;
import org.ikuzo.otboo.domain.clothes.dto.ClothesDto;

@Builder
public record RecommendationDto(
    UUID weatherId,
    UUID userId,
    List<ClothesDto> clothes
) {

}
