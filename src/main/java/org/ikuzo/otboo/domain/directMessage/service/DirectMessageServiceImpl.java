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
import org.ikuzo.otboo.global.event.message.MessageCreatedEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
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
}
