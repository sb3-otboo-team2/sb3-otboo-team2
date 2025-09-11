package org.ikuzo.otboo.domain.user.dto;

import java.util.UUID;

public record UserSummary(
    UUID userId,
    String name,
    String profileImageUrl
) {
}
