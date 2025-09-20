package org.ikuzo.otboo.domain.feed.exception;

import java.util.Set;
import org.ikuzo.otboo.global.exception.ErrorCode;

public class FeedClothesUnmatchOwner extends FeedException {

    private FeedClothesUnmatchOwner(Set<?> unauthorizedIds) {
        super(ErrorCode.FEED_UNMATCH_CLOTHES_OWNER);
        addDetail("unauthorizedClothesIds", unauthorizedIds);
    }

    public static FeedClothesUnmatchOwner withUnauthorizedIds(Set<?> unauthorizedIds) {
        return new FeedClothesUnmatchOwner(unauthorizedIds);
    }
}
