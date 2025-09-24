package org.ikuzo.otboo.domain.user.service;

import java.util.UUID;
import org.ikuzo.otboo.domain.user.dto.ProfileDto;
import org.ikuzo.otboo.domain.user.dto.UserCreateRequest;
import org.ikuzo.otboo.domain.user.dto.UserDto;

public interface UserService {
    UserDto create(UserCreateRequest userCreateRequest);
    ProfileDto find(UUID userId);
}
