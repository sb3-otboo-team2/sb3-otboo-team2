package org.ikuzo.otboo.domain.feed.repository;

import java.util.List;
import java.util.UUID;
import org.ikuzo.otboo.domain.feed.entity.Feed;
import org.ikuzo.otboo.domain.feed.repository.dto.FeedSortKey;

public interface FeedCustomRepository {
    List<Feed> findFeedsWithCursor(String cursor,
                                   UUID idAfter,
                                   int limit,
                                   FeedSortKey sortKey,
                                   boolean ascending,
                                   String keywordLike,
                                   String skyStatusEqual,
                                   String precipitationTypeEqual,
                                   UUID authorIdEqual);

    long countFeeds(String keywordLike,
                    String skyStatusEqual,
                    String precipitationTypeEqual,
                    UUID authorIdEqual);
}
