package org.ikuzo.otboo.domain.feed.exception;

import java.util.UUID;
import org.ikuzo.otboo.global.exception.ErrorCode;

public class FeedAuthorUnmatchException extends FeedException {
    public FeedAuthorUnmatchException(UUID authorId) {
        super(ErrorCode.FEED_UNMATCH_AUTHOR);
        addDetail("authorId", authorId);
    }
}
