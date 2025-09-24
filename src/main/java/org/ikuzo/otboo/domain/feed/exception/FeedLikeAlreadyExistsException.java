package org.ikuzo.otboo.domain.feed.exception;

import java.util.UUID;
import org.ikuzo.otboo.global.exception.ErrorCode;

public class FeedLikeAlreadyExistsException extends FeedException {

    private FeedLikeAlreadyExistsException(UUID feedId, UUID userId) {
        super(ErrorCode.FEED_LIKE_ALREADY_EXISTS);
        addDetail("feedId", feedId);
        addDetail("userId", userId);
    }

    public static FeedLikeAlreadyExistsException with(UUID feedId, UUID userId) {
        return new FeedLikeAlreadyExistsException(feedId, userId);
    }
}
