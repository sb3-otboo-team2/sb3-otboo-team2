package org.ikuzo.otboo.domain.user.service;

import java.util.Optional;
import java.util.UUID;
import org.ikuzo.otboo.domain.user.dto.ChangePasswordRequest;
import org.ikuzo.otboo.domain.user.dto.ProfileDto;
import org.ikuzo.otboo.domain.user.dto.ProfileUpdateRequest;
import org.ikuzo.otboo.domain.user.dto.UserCreateRequest;
import org.ikuzo.otboo.domain.user.dto.UserDto;
import org.ikuzo.otboo.domain.user.dto.UserRoleUpdateRequest;
import org.springframework.web.multipart.MultipartFile;

public interface UserService {
    UserDto create(UserCreateRequest userCreateRequest);
    ProfileDto find(UUID userId);
    ProfileDto update(UUID userId, ProfileUpdateRequest profileUpdateRequest, Optional<MultipartFile> image);
    UserDto updateRole(UUID userId, UserRoleUpdateRequest request);
    UserDto updateRoleInternal(UUID userId, UserRoleUpdateRequest request);
    void changePassword(UUID userId, ChangePasswordRequest request);
}
