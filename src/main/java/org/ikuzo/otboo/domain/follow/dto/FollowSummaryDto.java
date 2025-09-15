package org.ikuzo.otboo.domain.follow.dto;

import java.util.UUID;

/**
 * followSummaryDto
 *
 * @param followeeId: 팔로우 대상 UserID
 * @param followerCount: 팔로우 대상 팔로워 수
 * @param followingCount: 팔로워 대상 팔로잉 수
 * @param followedByMe: 팔로우 대상 사용자를 팔로우하고 있는지 여부
 * @param followedByMeId: 내가 팔로우 대상 사용자를 팔로우하고 있는 followId or null
 * @param followingMe: 팔로우 대상 사용자가 나를 팔로우하고 있는지 여부
 */
public record FollowSummaryDto(
    UUID followeeId,
    Long followerCount,
    Long followingCount,
    boolean followedByMe,
    UUID followedByMeId,
    boolean followingMe
) {
}
