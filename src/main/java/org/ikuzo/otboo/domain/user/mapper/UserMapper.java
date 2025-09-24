package org.ikuzo.otboo.domain.user.mapper;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.ikuzo.otboo.domain.feed.dto.AuthorDto;
import org.ikuzo.otboo.domain.user.dto.Location;
import org.ikuzo.otboo.domain.user.dto.ProfileDto;
import org.ikuzo.otboo.domain.user.dto.UserDto;
import org.ikuzo.otboo.domain.user.entity.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

@Mapper(componentModel = "spring")
public interface UserMapper {
    UserDto toDto(User user);

    @Mapping(target = "userId", source = "id")
    @Mapping(target = "gender", expression = "java(user.getGender() != null ? user.getGender().name() : null)")
    @Mapping(target = "location", source = "user", qualifiedByName = "mapLocation")
    ProfileDto toProfileDto(User user);

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

    @Named("mapLocation")
    default Location mapLocation(User user) {
        if (user.getLocationNames() == null || user.getLocationNames().isBlank()) {
            return null;
        }

        List<String> locationNamesList = parseLocationNames(user.getLocationNames());

        return new Location(
            user.getLatitude(),
            user.getLongitude(),
            user.getX(),
            user.getY(),
            locationNamesList
        );
    }

    default List<String> parseLocationNames(String locationNames) {
        if (locationNames == null || locationNames.isBlank()) {
            return Collections.emptyList();
        }

        // 정규표현식 \\s+로 하나 이상의 공백 문자를 기준으로 분리
        return Arrays.asList(locationNames.split("\\s+"));
    }
}
