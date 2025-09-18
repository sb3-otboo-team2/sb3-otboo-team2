package org.ikuzo.otboo.global.event.message;

import org.ikuzo.otboo.domain.directMessage.dto.DirectMessageDto;

import java.time.Instant;

public class MessageCreatedEvent extends CreatedEvent<DirectMessageDto> {
    public MessageCreatedEvent(DirectMessageDto data, Instant createdAt) {
        super(data, createdAt);
    }
}
