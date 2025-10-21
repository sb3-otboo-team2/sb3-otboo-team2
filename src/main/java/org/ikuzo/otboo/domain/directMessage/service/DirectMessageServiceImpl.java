package org.ikuzo.otboo.domain.directMessage.service;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.ikuzo.otboo.domain.directMessage.dto.DirectMessageCreateRequest;
import org.ikuzo.otboo.domain.directMessage.dto.DirectMessageDto;
import org.ikuzo.otboo.domain.directMessage.entity.DirectMessage;
import org.ikuzo.otboo.domain.directMessage.mapper.DirectMessageMapper;
import org.ikuzo.otboo.domain.directMessage.repository.DirectMessageRepository;
import org.ikuzo.otboo.domain.notification.service.NotificationService;
import org.ikuzo.otboo.domain.user.entity.User;
import org.ikuzo.otboo.domain.user.exception.UserNotFoundException;
import org.ikuzo.otboo.domain.user.repository.UserRepository;
import org.ikuzo.otboo.global.dto.PageResponse;
import org.ikuzo.otboo.global.event.message.MessageCreatedEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DirectMessageServiceImpl implements DirectMessageService {

    private final DirectMessageRepository directMessageRepository;
    private final UserRepository userRepository;
    private final DirectMessageMapper directMessageMapper;
    private final ApplicationEventPublisher eventPublisher;
    private final NotificationService notificationService;

    @Override
    @Transactional
    public DirectMessageDto sendMessage(DirectMessageCreateRequest directMessageCreateRequest) {
        UUID senderId = directMessageCreateRequest.senderId();
        UUID receiverId = directMessageCreateRequest.receiverId();

        User sender = userRepository.findById(senderId).orElseThrow(() -> new EntityNotFoundException());
        User receiver = userRepository.findById(receiverId).orElseThrow(() -> new EntityNotFoundException());

        DirectMessage message = DirectMessage.builder()
            .sender(sender)
            .receiver(receiver)
            .content(directMessageCreateRequest.content())
            .build();

        DirectMessage savedMessage = directMessageRepository.save(message);

        DirectMessageDto dto = directMessageMapper.toDto(savedMessage);

        // 웹소켓 실시간 전송을 위한 이벤트 발행
        eventPublisher.publishEvent(
            new MessageCreatedEvent(
                dto, Instant.now()
            )
        );

        // DM 알림 생성
        String title = "\"" + sender.getName() + "\"님이 메세지를 보냈어요.";
        String content = directMessageCreateRequest.content();
        notificationService.create(
            java.util.Set.of(receiverId), 
            title, 
            content, 
            org.ikuzo.otboo.domain.notification.entity.Level.INFO
        );

        return dto;
    }

    /**
     * DM 목록 조회
     *
     * @param userId: 메세지를 보낼(수신자) userId
     * @param cursor: 커서 (2025-09-10T09:47:14.318813Z)
     * @param idAfter: 보조 커서 (0f6a481b-aee8-41ce-8aaf-2cf76434b395)
     * @param limit: 사이즈
     *
     * @return PageResponse<DirectMessageDto>
     */
    @Override
    @Transactional(readOnly = true)
    public PageResponse<DirectMessageDto> getDirectMessages(UUID userId, Instant cursor, UUID idAfter, int limit) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String email = authentication.getName();
        User currentUser = userRepository.findByEmail(email).orElseThrow(UserNotFoundException::new);

        List<DirectMessage> directMessages = directMessageRepository.getDirectMessages(currentUser.getId(), userId, cursor, idAfter, limit);
        List<DirectMessage> list = directMessages.size() > limit ? directMessages.subList(0, limit) : directMessages;
        boolean hasNext = directMessages.size() > limit;
        Instant nextCursor = null;
        UUID nextIdAfter = null;

        if (hasNext && !list.isEmpty()) {
            DirectMessage last = list.get(list.size() - 1);
            nextCursor = last.getCreatedAt();
            nextIdAfter = last.getId();
        }
        String sortBy = "createdAt";
        String sortDirection = "DESCENDING";
        long totalCount = directMessageRepository.countDirectMessages(currentUser.getId(), userId);

        List<DirectMessageDto> content = list.stream()
            .map(directMessageMapper::toDto)
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
}
