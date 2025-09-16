package org.ikuzo.otboo.global.event.message;

import org.ikuzo.otboo.domain.directMessage.dto.DirectMessageDto;

import java.time.Instant;

public record MessageCreatedEvent(
    DirectMessageDto dto,
    Instant createdAt
) {
}
