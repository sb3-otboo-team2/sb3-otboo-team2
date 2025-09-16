package org.ikuzo.otboo.domain.directMessage.dto;

import org.ikuzo.otboo.domain.user.dto.UserSummary;

import java.time.Instant;
import java.util.UUID;

public record DirectMessageDto(
    UUID id,
    Instant createdAt,
    UserSummary sender,
    UserSummary receiver,
    String content
) {
}
