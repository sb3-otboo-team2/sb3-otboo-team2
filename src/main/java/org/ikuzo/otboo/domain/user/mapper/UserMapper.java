package org.ikuzo.otboo.domain.user.mapper;

import org.ikuzo.otboo.domain.feed.dto.AuthorDto;
import org.ikuzo.otboo.domain.user.dto.UserDto;
import org.ikuzo.otboo.domain.user.entity.User;
import org.mapstruct.Mapper;
import org.mapstruct.Named;

@Mapper(componentModel = "spring")
public interface UserMapper {
    UserDto toDto(User user);

    @Named("authorDto")
    default AuthorDto toAuthorDto(User user) {
        if (user == null) {
            return null;
        }
        return AuthorDto.builder()
            .userId(user.getId())
            .name(user.getName())
            .profileImageUrl(user.getProfileImageUrl())
            .build();
    }
}
