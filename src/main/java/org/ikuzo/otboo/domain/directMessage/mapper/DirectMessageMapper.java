package org.ikuzo.otboo.domain.directMessage.mapper;

import org.ikuzo.otboo.domain.directMessage.dto.DirectMessageDto;
import org.ikuzo.otboo.domain.directMessage.entity.DirectMessage;
import org.ikuzo.otboo.domain.user.dto.UserSummary;
import org.springframework.stereotype.Component;

@Component
public class DirectMessageMapper {

    public DirectMessageDto toDto(DirectMessage directMessage) {
        UserSummary sender = new UserSummary(
            directMessage.getSender().getId(),
            directMessage.getSender().getName(),
            directMessage.getSender().getProfileImageUrl()
        );
        
        UserSummary receiver = new UserSummary(
            directMessage.getReceiver().getId(),
            directMessage.getReceiver().getName(),
            directMessage.getReceiver().getProfileImageUrl()
        );

        return new DirectMessageDto(
            directMessage.getId(),
            directMessage.getCreatedAt(),
            sender,
            receiver,
            directMessage.getContent()
        );
    }
}
