package org.ikuzo.otboo.domain.notification.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ikuzo.otboo.domain.notification.dto.NotificationDto;
import org.ikuzo.otboo.domain.notification.entity.Level;
import org.ikuzo.otboo.domain.notification.entity.Notification;
import org.ikuzo.otboo.domain.notification.exception.NotificationNotFoundException;
import org.ikuzo.otboo.domain.notification.mapper.NotificationMapper;
import org.ikuzo.otboo.domain.notification.repository.NotificationRepository;
import org.ikuzo.otboo.domain.user.entity.User;
import org.ikuzo.otboo.domain.user.exception.UserNotFoundException;
import org.ikuzo.otboo.domain.user.repository.UserRepository;
import org.ikuzo.otboo.global.dto.PageResponse;
import org.ikuzo.otboo.global.event.message.NotificationCreatedEvent;
import org.ikuzo.otboo.global.security.OtbooUserDetails;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
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
    private final UserRepository userRepository;

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @CacheEvict(cacheNames = "notifications", allEntries = true)
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

    @Override
    @Transactional(readOnly = true)
    @Cacheable(cacheNames = "notifications",
        key = "T(org.springframework.security.core.context.SecurityContextHolder).getContext().getAuthentication().getName()",
        condition = "#cursor == null && #idAfter == null"
    )
    public PageResponse<NotificationDto> getNotifications(Instant cursor, UUID idAfter, int limit) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        OtbooUserDetails principal = (OtbooUserDetails) authentication.getPrincipal();
        String userEmail = principal.getUsername();

        User user = userRepository.findByEmail(userEmail).orElseThrow(UserNotFoundException::new);

        log.info("userId={}", user.getId());
        List<Notification> list = notificationRepository.findByCursor(user.getId(), cursor, idAfter, limit);
        List<Notification> notifications = list.size() > limit ? list.subList(0, limit) : list;

        boolean hasNext = list.size() > limit;
        Instant nextCursor = null;
        UUID nextIdAfter = null;

        if (hasNext && !list.isEmpty()) {
            Notification last = notifications.get(notifications.size() - 1);
            nextCursor = last.getCreatedAt();
            nextIdAfter = last.getId();
        }
        String sortBy = "createdAt";
        String sortDirection = "DESCENDING";

        long totalCount = notificationRepository.countByReceiverId(user.getId());

        List<NotificationDto> content = notifications.stream()
            .map(notificationMapper::toDto)
            .toList();

        return new PageResponse<>(
            content,
            nextCursor,
            nextIdAfter,
            hasNext,
            totalCount,
            sortBy,
            sortDirection
        );
    }

    @Override
    @Transactional
    @CacheEvict(cacheNames = "notifications", key = "T(org.springframework.security.core.context.SecurityContextHolder).getContext().getAuthentication().getName()")
    public void deleteNotification(UUID notificationId) {
        Notification notification = notificationRepository.findById(notificationId).orElseThrow(() -> NotificationNotFoundException.notFoundException(notificationId));

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        OtbooUserDetails principal = (OtbooUserDetails) authentication.getPrincipal();
        UUID currentId = principal.getUserDto().id();

        if (!notification.getReceiverId().equals(currentId)) {
            throw new AuthorizationDeniedException("삭제할 권한이 없습니다.");
        }

        notificationRepository.delete(notification);
    }
}
