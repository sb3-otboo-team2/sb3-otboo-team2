package org.ikuzo.otboo.domain.notification.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ikuzo.otboo.domain.notification.dto.NotificationDto;
import org.ikuzo.otboo.domain.notification.entity.Level;
import org.ikuzo.otboo.domain.notification.entity.Notification;
import org.ikuzo.otboo.domain.notification.mapper.NotificationMapper;
import org.ikuzo.otboo.domain.notification.reppsitory.NotificationRepository;
import org.ikuzo.otboo.global.event.message.NotificationCreatedEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {

    private final NotificationRepository notificationRepository;
    private final NotificationMapper notificationMapper;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @Override
    public void create(Set<UUID> receiverIds, String title, String content, Level level) {
        if (receiverIds.isEmpty()) {
            log.warn("알림 생성 요청이 비어있음: receiverIds={}", receiverIds);
            return;
        }
        log.debug("새 알림 생성 시작: receiverIds={}", receiverIds);
        List<Notification> notifications = receiverIds.stream()
            .map(receiverId -> Notification.builder()
                .receiverId(receiverId)
                .title(title)
                .content(content)
                .level(level)
                .build()
            ).toList();
        notificationRepository.saveAll(notifications);

        List<NotificationDto> createdNotifications = notifications.stream()
            .map(notificationMapper::toDto)
            .toList();
        eventPublisher.publishEvent(
            new NotificationCreatedEvent(createdNotifications, Instant.now())
        );
        log.info("새 알림 생성 완료: receiverIds={}", receiverIds);
    }
}
