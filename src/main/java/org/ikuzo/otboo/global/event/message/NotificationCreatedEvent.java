package org.ikuzo.otboo.global.event.message;

import org.ikuzo.otboo.domain.notification.dto.NotificationDto;

import java.time.Instant;
import java.util.List;

public record NotificationCreatedEvent(
    List<NotificationDto> dto,
    Instant createdAt
) {
}
