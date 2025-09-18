package org.ikuzo.otboo.domain.notification.dto;

import org.ikuzo.otboo.domain.notification.entity.Level;

import java.time.Instant;
import java.util.UUID;

public record NotificationDto(
    UUID id,
    Instant createdAt,
    UUID receiverId,
    String title,
    String content,
    Level level
) {
}
