package org.ikuzo.otboo.domain.user.service;

import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ikuzo.otboo.domain.user.dto.ChangePasswordRequest;
import org.ikuzo.otboo.domain.user.dto.ProfileDto;
import org.ikuzo.otboo.domain.user.dto.ProfileUpdateRequest;
import org.ikuzo.otboo.domain.user.dto.UserCreateRequest;
import org.ikuzo.otboo.domain.user.dto.UserDto;
import org.ikuzo.otboo.domain.user.dto.UserRoleUpdateRequest;
import org.ikuzo.otboo.domain.user.entity.Role;
import org.ikuzo.otboo.domain.user.entity.User;
import org.ikuzo.otboo.domain.user.exception.UserAlreadyExistsException;
import org.ikuzo.otboo.domain.user.exception.UserNotFoundException;
import org.ikuzo.otboo.domain.user.mapper.UserMapper;
import org.ikuzo.otboo.domain.user.repository.UserRepository;
import org.ikuzo.otboo.global.util.ImageSwapHelper;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@RequiredArgsConstructor
@Service
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserMapper userMapper;
    private final ImageSwapHelper imageSwapHelper;

    @Transactional
    @Override
    public UserDto create(UserCreateRequest userCreateRequest) {
        log.debug("사용자 생성 시작");

        // 중복 이메일 확인
        String email = userCreateRequest.email();
        if (userRepository.existsByEmail(email)) {
            throw UserAlreadyExistsException.withEmail(email);
        }

        // 비밀번호 암호화
        String encodedPassword = passwordEncoder.encode(userCreateRequest.password());

        // 유저 생성 및 저장
        User user = new User(email, userCreateRequest.name(), encodedPassword);
        userRepository.save(user);

        log.info("사용자 생성 완료: id={}, name={}", user.getId(), user.getName());

        return userMapper.toDto(user);
    }

    @Override
    public ProfileDto find(UUID userId) {
        return userRepository.findById(userId)
            .map(userMapper::toProfileDto)
            .orElseThrow(() -> UserNotFoundException.withId(userId));
    }

    @PreAuthorize("principal.userDto.id == #userId")
    @Transactional
    @Override
    public ProfileDto update(UUID userId, ProfileUpdateRequest profileUpdateRequest, Optional<MultipartFile> image) {
        log.debug("사용자 프로필 수정 시작: id={}", userId);

        User user = userRepository.findById(userId)
            .orElseThrow(() -> {
                UserNotFoundException exception = UserNotFoundException.withId(userId);
                return exception;
            });

        String newProfileImageUrl = image.map(img -> {
            log.debug("프로필 이미지 업로드 시작");
            return imageSwapHelper.swapImageSafely("profileImage/", img, user.getProfileImageUrl(), userId);
        }).orElse(null);

        user.update(
            profileUpdateRequest.name(),
            profileUpdateRequest.gender(),
            profileUpdateRequest.birthDate(),
            profileUpdateRequest.location(),
            profileUpdateRequest.temperatureSensitivity(),
            newProfileImageUrl
        );

        log.info("사용자 프로필 수정 완료: id={}", userId);

        return userMapper.toProfileDto(user);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    @Override
    public UserDto updateRole(UUID userId, UserRoleUpdateRequest request) {
        return updateRoleInternal(userId, request);
    }

    @Transactional
    @Override
    public UserDto updateRoleInternal(UUID userId, UserRoleUpdateRequest request) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> UserNotFoundException.withId(userId));

        Role newRole = request.role();
        user.updateRole(newRole);

        return userMapper.toDto(user);
    }

    @PreAuthorize("principal.userDto.id == #userId")
    @Transactional
    @Override
    public void changePassword(UUID userId, ChangePasswordRequest request) {
        log.debug("비밀번호 변경 시작: id={}", userId);

        User user = userRepository.findById(userId)
            .orElseThrow(() -> {
                UserNotFoundException exception = UserNotFoundException.withId(userId);
                return exception;
            });

        String newPassword = request.password();
        String encodedPassword = Optional.ofNullable(newPassword).map(passwordEncoder::encode)
            .orElse(user.getPassword());

        user.changePassword(encodedPassword);

        log.info("비밀번호 변경 완료: id={}", userId);
    }

}
