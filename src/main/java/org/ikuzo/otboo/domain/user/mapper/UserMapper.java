package org.ikuzo.otboo.domain.user.mapper;

import org.ikuzo.otboo.domain.user.dto.UserDto;
import org.ikuzo.otboo.domain.user.entity.User;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface UserMapper {
    UserDto toDto(User user);
}
