package org.ikuzo.otboo.domain.feed.dto;

import java.util.UUID;
import org.ikuzo.otboo.domain.user.dto.UserSummary;

public record FeedCreatedEventDto(
    UUID feedId,
    String content,
    UserSummary author
) {
}