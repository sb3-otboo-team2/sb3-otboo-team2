package org.ikuzo.otboo.global.event.message;

import org.ikuzo.otboo.domain.follow.dto.FollowDto;

import java.time.Instant;

public record FollowCreatedEvent(
    FollowDto dto,
    Instant createdAt
) {
}
