package org.ikuzo.otboo.domain.follow.repository;

import org.ikuzo.otboo.domain.follow.dto.FollowDto;
import org.ikuzo.otboo.domain.follow.entity.Follow;

import java.util.List;
import java.util.UUID;

public interface FollowCustomRepository {

    List<Follow> getFollows(UUID followeeId, String cursor, UUID idAfter, int limit, String nameLike, String type);

    long countByCursorFilter(UUID followeeId, String nameLike, String type);

}
