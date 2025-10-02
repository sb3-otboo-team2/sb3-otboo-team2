package org.ikuzo.otboo.global.event.message;

import java.time.Instant;
import org.ikuzo.otboo.domain.feed.dto.FeedCreatedEventDto;

public class FeedCreatedEvent extends CreatedEvent<FeedCreatedEventDto> {
    public FeedCreatedEvent(FeedCreatedEventDto data, Instant createdAt) {
        super(data, createdAt);
    }
}