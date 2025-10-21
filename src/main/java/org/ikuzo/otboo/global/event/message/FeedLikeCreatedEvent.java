package org.ikuzo.otboo.global.event.message;

import java.time.Instant;
import org.ikuzo.otboo.domain.feedLike.dto.FeedLikeEventDto;

public class FeedLikeCreatedEvent extends CreatedEvent<FeedLikeEventDto> {
    public FeedLikeCreatedEvent(FeedLikeEventDto data, Instant createdAt) {
        super(data, createdAt);
    }
}