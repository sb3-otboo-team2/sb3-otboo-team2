package org.ikuzo.otboo.domain.feed.exception;

import java.util.UUID;
import org.ikuzo.otboo.global.exception.ErrorCode;

public class FeedNotFoundException extends FeedException {
    public FeedNotFoundException(UUID feedId) {
        super(ErrorCode.FEED_NOT_FOUND);
        addDetail("feedId", feedId);
    }
}
