package org.ikuzo.otboo.domain.directMessage.dto;

import java.util.UUID;

public record DirectMessageCreateRequest(
    UUID receiverId,
    UUID senderId,
    String content
) {
}
