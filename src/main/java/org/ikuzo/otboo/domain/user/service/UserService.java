package org.ikuzo.otboo.domain.user.service;

import org.ikuzo.otboo.domain.user.dto.UserCreateRequest;
import org.ikuzo.otboo.domain.user.dto.UserDto;

public interface UserService {
    UserDto create(UserCreateRequest userCreateRequest);
}
