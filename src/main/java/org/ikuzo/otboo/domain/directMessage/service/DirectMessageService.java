package org.ikuzo.otboo.domain.directMessage.service;

import org.ikuzo.otboo.domain.directMessage.dto.DirectMessageCreateRequest;
import org.ikuzo.otboo.domain.directMessage.dto.DirectMessageDto;

public interface DirectMessageService {
    DirectMessageDto sendMessage(DirectMessageCreateRequest directMessageCreateRequest);
}
