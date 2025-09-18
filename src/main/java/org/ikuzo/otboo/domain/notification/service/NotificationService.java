package org.ikuzo.otboo.domain.notification.service;

import org.ikuzo.otboo.domain.notification.entity.Level;

import java.util.Set;
import java.util.UUID;

public interface NotificationService {

    void create(Set<UUID> receiverIds, String title, String content, Level level);
}
