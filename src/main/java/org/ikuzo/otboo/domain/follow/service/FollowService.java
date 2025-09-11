package org.ikuzo.otboo.domain.follow.service;

import org.ikuzo.otboo.domain.follow.dto.FollowCreateRequest;
import org.ikuzo.otboo.domain.follow.dto.FollowDto;

public interface FollowService {
    FollowDto follow(FollowCreateRequest request);
}
