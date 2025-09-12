package org.ikuzo.otboo.domain.follow.service;

import org.ikuzo.otboo.domain.follow.dto.FollowCreateRequest;
import org.ikuzo.otboo.domain.follow.dto.FollowDto;
import org.ikuzo.otboo.domain.follow.dto.FollowSummaryDto;

import java.util.UUID;

public interface FollowService {
    FollowDto follow(FollowCreateRequest request);
    FollowSummaryDto followSummary(UUID userId);
}
