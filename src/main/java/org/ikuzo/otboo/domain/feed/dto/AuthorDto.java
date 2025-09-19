package org.ikuzo.otboo.domain.feed.dto;

import java.util.UUID;
import lombok.Builder;

@Builder
public record AuthorDto(
    UUID userId,
    String name,
    String profileImageUrl
) {
}