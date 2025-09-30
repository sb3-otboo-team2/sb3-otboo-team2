package org.ikuzo.otboo.domain.user.service;

import java.util.List;
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
import org.ikuzo.otboo.global.dto.PageResponse;
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

    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    @Override
    public PageResponse<UserDto> getUsers(
        String cursor,
        UUID idAfter,
        Integer limit,
        String sortBy,
        String sortDirection,
        String emailLike,
        String roleEqual,
        Boolean locked
    ) {
        log.debug("계정 목록 조회 시작");

        // 커서 기반으로 사용자 조회 (limit+1개 조회)
        List<User> users = userRepository.findUsersWithCursor(
            cursor,
            idAfter,
            limit,
            sortBy,
            sortDirection,
            emailLike,
            roleEqual,
            locked
        );

        // 다음 페이지 존재 여부 판단
        boolean hasNext = users.size() > limit;
        if (hasNext) {
            users = users.subList(0, limit); // 실제 반환할 데이터만 남김
        }

        // Entity -> DTO 변환 (MapStruct 사용)
        List<UserDto> userDtos = users.stream()
            .map(userMapper::toDto) // UserMapper.toDto() 호출
            .toList();

        // 다음 커서 계산
        Object nextCursor = null;
        UUID nextIdAfter = null;

        if (hasNext && !users.isEmpty()) {
            User lastUser = users.get(users.size() - 1); // 마지막 데이터
            nextCursor = getNextCursorValue(lastUser, sortBy);
            nextIdAfter = lastUser.getId();
        }

        // 전체 개수 카운트 (필터 조건 동일하게 적용)
        Long totalCount = userRepository.countUsersWithFilters(
            emailLike,
            roleEqual,
            locked
        );

        // 응답 DTO 생성
        return new PageResponse<>(
            userDtos,       // 실제 데이터
            nextCursor,     // 다음 페이지 커서
            nextIdAfter,    // 다음 페이지 시작 ID
            hasNext,        // 다음 페이지 존재 여부
            totalCount,     // 전체 데이터 개수
            sortBy,         // 정렬 기준
            sortDirection   // 정렬 방향
        );
    }

    /**
     * 정렬 기준에 따라 다음 커서 값 추출
     *
     * @param user 마지막으로 조회된 사용자
     * @param sortBy 정렬 기준 필드
     * @return 다음 페이지 요청 시 사용할 커서 값
     */
    private Object getNextCursorValue(User user, String sortBy) {
        return switch (sortBy) {
            case "email" -> user.getEmail(); // 이메일 문자열
            case "createdAt" -> user.getCreatedAt().toString(); // ISO-8601 형식 문자열
            default -> user.getId().toString(); // UUID 문자열
        };
    }

}
