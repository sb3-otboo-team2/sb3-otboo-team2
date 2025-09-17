package org.ikuzo.otboo.domain.directMessage.service;

import org.ikuzo.otboo.domain.directMessage.dto.DirectMessageCreateRequest;
import org.ikuzo.otboo.domain.directMessage.dto.DirectMessageDto;
import org.ikuzo.otboo.global.dto.PageResponse;

import java.time.Instant;
import java.util.UUID;

public interface DirectMessageService {
    DirectMessageDto sendMessage(DirectMessageCreateRequest directMessageCreateRequest);

    PageResponse<DirectMessageDto> getDirectMessages(UUID userId, Instant cursor, UUID idAfter, int limit);
}
