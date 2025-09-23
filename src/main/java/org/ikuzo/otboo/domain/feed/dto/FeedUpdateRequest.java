package org.ikuzo.otboo.domain.feed.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record FeedUpdateRequest(
    @NotBlank(message = "내용은 필수입니다.")
    @Size(max = 100, message = "100자 미만이어야 합니다.")
    String content
) {
}
