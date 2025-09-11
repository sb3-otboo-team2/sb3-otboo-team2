package org.ikuzo.otboo.domain.follow.dto;

import org.ikuzo.otboo.domain.user.dto.UserSummary;

import java.util.UUID;

public record FollowDto(
    UUID id,
    UserSummary followee,
    UserSummary follower
) {
}
