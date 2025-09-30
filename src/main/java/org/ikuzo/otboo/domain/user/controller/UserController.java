package org.ikuzo.otboo.domain.user.controller;

import jakarta.validation.Valid;
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
import org.ikuzo.otboo.domain.user.service.UserService;
import org.ikuzo.otboo.global.dto.PageResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    @PostMapping
    public ResponseEntity<UserDto> create(
        @RequestBody @Valid UserCreateRequest userCreateRequest
    ) {
        log.info("사용자 생성 요청 수신");

        UserDto createdUser = userService.create(userCreateRequest);

        log.debug("사용자 생성 응답: {}", createdUser.id());

        return ResponseEntity.status(HttpStatus.CREATED).body(createdUser);
    }

    @GetMapping(path = "/{userId}/profiles")
    public ResponseEntity<ProfileDto> find(@PathVariable("userId") UUID userId) {
        log.info("사용자 프로필 조회 요청 수신");

        ProfileDto profile = userService.find(userId);

        log.debug("사용자 프로필 조회 응답: {}", userId);

        return ResponseEntity.status(HttpStatus.OK).body(profile);
    }

    @PatchMapping(
        path = "/{userId}/profiles",
        consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public ResponseEntity<ProfileDto> update(
        @PathVariable("userId") UUID userId,
        @RequestPart("request") @Valid ProfileUpdateRequest request,
        @RequestPart(value = "image", required = false) MultipartFile image
    ) {
        log.info("사용자 프로필 수정 요청: id={}, request={}", userId, request);

        ProfileDto updatedProfile = userService.update(userId, request, Optional.ofNullable(image));

        log.debug("사용자 프로필 수정 응답: {}", updatedProfile);

        return ResponseEntity.ok(updatedProfile);
    }

    @PatchMapping(path = "/{userId}/role")
    public ResponseEntity<UserDto> role(
        @PathVariable("userId") UUID userId,
        @RequestBody UserRoleUpdateRequest request
    ) {
        log.info("권한 수정 요청");
        UserDto userDto = userService.updateRole(userId, request);
        return ResponseEntity.ok(userDto);
    }

    @PatchMapping(path = "/{userId}/password")
    public ResponseEntity<Void> changePassword(
        @PathVariable("userId") UUID userId,
        @RequestBody ChangePasswordRequest request
    ) {
        log.info("비밀번호 변경 요청: id={}", userId);

        userService.changePassword(userId, request);

        return ResponseEntity.noContent().build();
    }

    @GetMapping
    public ResponseEntity<PageResponse<UserDto>> getUsers(
        @RequestParam(required = false) String cursor,
        @RequestParam(required = false) UUID idAfter,
        @RequestParam(defaultValue = "10") Integer limit,
        @RequestParam(defaultValue = "email") String sortBy,
        @RequestParam(defaultValue = "DESCENDING") String sortDirection,
        @RequestParam(required = false) String emailLike,
        @RequestParam(required = false) String roleEqual,
        @RequestParam(required = false) Boolean locked
    ) {
        log.info("계정 목록 조회 요청 - sortBy: {}, sortDirection: {}, emailLike: {}, roleEqual: {}, locked: {}",
            sortBy, sortDirection, emailLike, roleEqual, locked);

        PageResponse<UserDto> response = userService.getUsers(
            cursor,
            idAfter,
            limit,
            sortBy,
            sortDirection,
            emailLike,
            roleEqual,
            locked
        );

        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

}
