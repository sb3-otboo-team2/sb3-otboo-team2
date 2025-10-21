package org.ikuzo.otboo.domain.notification.service;

import org.ikuzo.otboo.domain.notification.dto.NotificationDto;
import org.ikuzo.otboo.domain.notification.entity.Level;
import org.ikuzo.otboo.global.dto.PageResponse;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public interface NotificationService {

    void create(Set<UUID> receiverIds, String title, String content, Level level);

    PageResponse<NotificationDto> getNotifications(Instant cursor, UUID idAfter, int limit);

    void deleteNotification(UUID notificationId);
}
