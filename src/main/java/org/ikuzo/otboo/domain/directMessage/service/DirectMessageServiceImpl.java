package org.ikuzo.otboo.domain.directMessage.service;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.ikuzo.otboo.domain.directMessage.dto.DirectMessageCreateRequest;
import org.ikuzo.otboo.domain.directMessage.dto.DirectMessageDto;
import org.ikuzo.otboo.domain.directMessage.entity.DirectMessage;
import org.ikuzo.otboo.domain.directMessage.mapper.DirectMessageMapper;
import org.ikuzo.otboo.domain.directMessage.repository.DirectMessageRepository;
import org.ikuzo.otboo.domain.user.entity.User;
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

        eventPublisher.publishEvent(
            new MessageCreatedEvent(
                dto, Instant.now()
            )
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
        // TODO: SpringSecurity 개발 후 securityContextHolder를 통해 접속 중인 유저 조회
//        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
//        String email = authentication.getName();
//        userRepository.findByEmail(email).orElseThrow(() -> new EntityNotFoundException("유저를 찾을 수 없습니다."));

        UUID currentUserId = UUID.fromString("94728e4f-c0c6-40eb-99d1-f6bf07d2aee1");

        List<DirectMessage> directMessages = directMessageRepository.getDirectMessages(currentUserId, userId, cursor, idAfter, limit);
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
        long totalCount = directMessageRepository.countDirectMessages(currentUserId, userId);

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
