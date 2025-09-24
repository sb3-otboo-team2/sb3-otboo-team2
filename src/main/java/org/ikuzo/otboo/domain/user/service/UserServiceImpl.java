package org.ikuzo.otboo.domain.user.service;

import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ikuzo.otboo.domain.user.dto.ProfileDto;
import org.ikuzo.otboo.domain.user.dto.UserCreateRequest;
import org.ikuzo.otboo.domain.user.dto.UserDto;
import org.ikuzo.otboo.domain.user.entity.User;
import org.ikuzo.otboo.domain.user.exception.UserAlreadyExistsException;
import org.ikuzo.otboo.domain.user.exception.UserNotFoundException;
import org.ikuzo.otboo.domain.user.mapper.UserMapper;
import org.ikuzo.otboo.domain.user.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@RequiredArgsConstructor
@Service
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserMapper userMapper;

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
}
