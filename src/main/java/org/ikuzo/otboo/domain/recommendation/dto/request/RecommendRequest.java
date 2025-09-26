package org.ikuzo.otboo.domain.recommendation.dto.request;

import java.util.List;
import java.util.UUID;

public record RecommendRequest(
    UUID userId,
    String gender,
    Integer tempSensitivity,
    Double temperature,
    Double humidity,
    String skyStatus,
    String precipitationType,
    String WindType,
    List<WardrobeItem> wardrobe
) {

    public record WardrobeItem(
        UUID id,
        String name,
        String type,
        List<String> attributes
    ) {

    }
}
