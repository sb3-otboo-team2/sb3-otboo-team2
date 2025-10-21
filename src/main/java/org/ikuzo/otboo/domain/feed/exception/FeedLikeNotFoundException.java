package org.ikuzo.otboo.domain.feed.exception;

import org.ikuzo.otboo.global.exception.ErrorCode;

public class FeedLikeNotFoundException extends FeedException {
    public FeedLikeNotFoundException() {
        super(ErrorCode.FEED_LIKE_NOT_FOUND);
    }
}
