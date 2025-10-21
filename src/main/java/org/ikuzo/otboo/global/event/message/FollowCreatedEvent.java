package org.ikuzo.otboo.global.event.message;

import org.ikuzo.otboo.domain.follow.dto.FollowDto;

import java.time.Instant;

public class FollowCreatedEvent extends CreatedEvent<FollowDto> {
    public FollowCreatedEvent(FollowDto data, Instant createdAt) {
        super(data, createdAt);
    }
}