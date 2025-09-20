package org.ikuzo.otboo.domain.feed.exception;

import org.ikuzo.otboo.global.exception.ErrorCode;

public class FeedClothesUnmatchOwner extends FeedException {
    public FeedClothesUnmatchOwner() {
        super(ErrorCode.FEED_UNMATCH_CLOTHES_OWNER);
    }
}
