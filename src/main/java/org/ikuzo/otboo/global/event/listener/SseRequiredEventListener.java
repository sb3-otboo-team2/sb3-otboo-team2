package org.ikuzo.otboo.global.event.listener;

import lombok.RequiredArgsConstructor;
import org.ikuzo.otboo.domain.notification.dto.NotificationDto;
import org.ikuzo.otboo.global.event.message.NotificationCreatedEvent;
import org.ikuzo.otboo.global.sse.service.SseService;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.List;
import java.util.Set;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class SseRequiredEventListener {

    private final SseService sseService;

    @TransactionalEventListener
    public void on(NotificationCreatedEvent event) {
        List<NotificationDto> notifications = event.dto();

        notifications.forEach(notification -> {
            UUID receiverId = notification.receiverId();
            sseService.send(Set.of(receiverId), "notifications.created", notification);
        });
    }
}
