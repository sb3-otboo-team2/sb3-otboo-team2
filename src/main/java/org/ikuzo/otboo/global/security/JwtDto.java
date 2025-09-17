package org.ikuzo.otboo.global.security;

import org.ikuzo.otboo.domain.user.dto.UserDto;

public record JwtDto(
    UserDto userDto,
    String accessToken
) {

}