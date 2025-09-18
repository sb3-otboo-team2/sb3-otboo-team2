package org.ikuzo.otboo.global.event.message;

import org.ikuzo.otboo.domain.notification.dto.NotificationDto;

import java.time.Instant;
import java.util.List;

public class NotificationCreatedEvent extends CreatedEvent<List<NotificationDto>> {
    public NotificationCreatedEvent(List<NotificationDto> data, Instant createdAt) {
        super(data, createdAt);
    }
}
