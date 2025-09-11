package org.ikuzo.otboo.domain.follow.mapper;

import org.ikuzo.otboo.domain.follow.dto.FollowDto;
import org.ikuzo.otboo.domain.follow.entity.Follow;
import org.ikuzo.otboo.domain.user.dto.UserSummary;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface FollowMapper {

    FollowDto toDto(Follow follow, UserSummary follower, UserSummary followee);
}
