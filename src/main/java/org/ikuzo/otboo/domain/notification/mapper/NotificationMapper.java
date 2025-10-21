package org.ikuzo.otboo.domain.notification.mapper;

import org.ikuzo.otboo.domain.notification.dto.NotificationDto;
import org.ikuzo.otboo.domain.notification.entity.Notification;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface NotificationMapper {
    NotificationDto toDto(Notification notification);
}
