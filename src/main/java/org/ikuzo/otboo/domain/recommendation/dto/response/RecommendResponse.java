package org.ikuzo.otboo.domain.recommendation.dto.response;

import java.util.List;
import java.util.UUID;

public record RecommendResponse(
    List<Pick> picks,
    String reasoning
) {
    public record Pick(
        UUID id,
        Double score,
        String reason
    ) {}
}
