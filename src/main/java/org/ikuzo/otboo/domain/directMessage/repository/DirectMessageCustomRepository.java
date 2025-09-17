package org.ikuzo.otboo.domain.directMessage.repository;

import org.ikuzo.otboo.domain.directMessage.entity.DirectMessage;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface DirectMessageCustomRepository {
    List<DirectMessage>  getDirectMessages(UUID currentId, UUID userId, Instant cursor, UUID idAfter, int limit);
    long countDirectMessages(UUID currentId, UUID userId);
}
