package org.ikuzo.otboo.domain.feed.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record FeedUpdateRequest(
    @NotNull
    @Size(max = 100, message = "100자 미만이어야 합니다.")
    String content
) {
}
