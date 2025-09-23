package org.ikuzo.otboo.domain.feed.service;

import java.util.UUID;
import org.ikuzo.otboo.domain.feed.dto.FeedCreateRequest;
import org.ikuzo.otboo.domain.feed.dto.FeedDto;
import org.ikuzo.otboo.domain.feed.dto.FeedUpdateRequest;
import org.ikuzo.otboo.global.dto.PageResponse;

public interface FeedService {
    FeedDto createFeed(FeedCreateRequest request);

    PageResponse<FeedDto> getFeeds(String cursor,
                                   UUID idAfter,
                                   Integer limit,
                                   String sortBy,
                                   String sortDirection,
                                   String keywordLike,
                                   String skyStatusEqual,
                                   String precipitationTypeEqual);

    FeedDto updateFeed(UUID feedId, FeedUpdateRequest request);
}
