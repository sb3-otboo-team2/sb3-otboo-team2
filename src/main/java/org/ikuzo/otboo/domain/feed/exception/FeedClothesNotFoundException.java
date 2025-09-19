package org.ikuzo.otboo.domain.feed.exception;

import java.util.Set;
import org.ikuzo.otboo.global.exception.ErrorCode;

public class FeedClothesNotFoundException extends FeedException {

    private FeedClothesNotFoundException(Set<?> missingIds) {
        super(ErrorCode.FEED_CLOTHES_NOT_FOUND);
        addDetail("missingClothesIds", missingIds);
    }

    public static FeedClothesNotFoundException withMissingIds(Set<?> missingIds) {
        return new FeedClothesNotFoundException(missingIds);
    }
}
