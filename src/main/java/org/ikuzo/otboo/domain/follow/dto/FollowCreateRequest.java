package org.ikuzo.otboo.domain.follow.dto;

import java.util.UUID;

public record FollowCreateRequest(
    UUID followeeId,
    UUID followerId
) {
}
