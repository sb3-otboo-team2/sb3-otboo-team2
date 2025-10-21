package org.ikuzo.otboo.domain.auth.dto;

import org.ikuzo.otboo.domain.user.dto.UserDto;

public record JwtDto(
    UserDto userDto,
    String accessToken
) {

}