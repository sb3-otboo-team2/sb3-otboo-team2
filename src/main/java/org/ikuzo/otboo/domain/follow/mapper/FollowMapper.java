package org.ikuzo.otboo.domain.follow.mapper;

import org.ikuzo.otboo.domain.follow.dto.FollowDto;
import org.ikuzo.otboo.domain.follow.entity.Follow;
import org.ikuzo.otboo.domain.user.dto.UserSummary;
import org.springframework.stereotype.Component;

@Component
public class FollowMapper {

    public FollowDto toDto(Follow follow, UserSummary followee, UserSummary follower) {
        return new FollowDto(
            follow.getId(),
            followee,
            follower
        );
    }
}
