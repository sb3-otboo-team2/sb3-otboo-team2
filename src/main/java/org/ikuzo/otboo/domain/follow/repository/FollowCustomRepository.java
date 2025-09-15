package org.ikuzo.otboo.domain.follow.repository;

import org.ikuzo.otboo.domain.follow.dto.FollowDto;
import org.ikuzo.otboo.domain.follow.entity.Follow;

import java.util.List;
import java.util.UUID;

public interface FollowCustomRepository {

    List<Follow> getFollowers(UUID followeeId, String cursor, UUID idAfter, int limit, String nameLike);

    long countByCursorFilter(UUID followeeId, String cursor, UUID idAfter, int limit, String nameLike);

    List<Follow> getFollowings(UUID followeeId, String cursor, UUID idAfter, int limit, String nameLike);
}
