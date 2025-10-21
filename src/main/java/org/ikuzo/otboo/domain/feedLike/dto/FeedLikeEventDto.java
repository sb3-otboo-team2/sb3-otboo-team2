package org.ikuzo.otboo.domain.feedLike.dto;

import java.util.UUID;
import org.ikuzo.otboo.domain.user.dto.UserSummary;

public record FeedLikeEventDto(
    UUID feedId,
    String feedContent,
    UserSummary author,
    UserSummary liker
) {
}