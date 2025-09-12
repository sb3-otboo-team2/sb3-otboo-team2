package org.ikuzo.otboo.domain.follow.service;

import org.ikuzo.otboo.domain.follow.dto.FollowCreateRequest;
import org.ikuzo.otboo.domain.follow.dto.FollowDto;
import org.ikuzo.otboo.domain.follow.dto.FollowSummaryDto;
import org.ikuzo.otboo.global.dto.PageResponse;

import java.util.UUID;

public interface FollowService {
    FollowDto follow(FollowCreateRequest request);
    FollowSummaryDto followSummary(UUID userId);
    PageResponse<FollowDto> getFollowers(UUID followeeId, String cursor, UUID idAfter, int limit, String nameLike);
}
