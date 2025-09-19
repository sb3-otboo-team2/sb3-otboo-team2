package org.ikuzo.otboo.domain.notification.repository;

import org.ikuzo.otboo.domain.notification.entity.Notification;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface NotificationCustomRepository {

    List<Notification> findByCursor(UUID userId, Instant cursor, UUID idAfter, int limit);
}
