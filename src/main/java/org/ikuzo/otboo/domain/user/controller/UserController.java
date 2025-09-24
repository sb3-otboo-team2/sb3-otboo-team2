package org.ikuzo.otboo.domain.user.controller;

import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ikuzo.otboo.domain.user.dto.ProfileDto;
import org.ikuzo.otboo.domain.user.dto.UserCreateRequest;
import org.ikuzo.otboo.domain.user.dto.UserDto;
import org.ikuzo.otboo.domain.user.service.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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

}
