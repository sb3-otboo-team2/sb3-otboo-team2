package org.ikuzo.otboo.domain.feed.service;

import org.ikuzo.otboo.domain.feed.dto.FeedCreateRequest;
import org.ikuzo.otboo.domain.feed.dto.FeedDto;

public interface FeedService {
    FeedDto createFeed(FeedCreateRequest request);
}
