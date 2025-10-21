package org.ikuzo.otboo.domain.recommendation.dto;

import java.util.List;
import java.util.UUID;
import lombok.Builder;

@Builder
public record RecommendationDto(
    UUID weatherId,
    UUID userId,
    List<OotdDto> clothes
) {

}
