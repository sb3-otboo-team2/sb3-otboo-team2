package org.ikuzo.otboo.global.security;


import lombok.RequiredArgsConstructor;
import org.ikuzo.otboo.domain.user.dto.UserDto;
import org.ikuzo.otboo.domain.user.entity.User;
import org.ikuzo.otboo.domain.user.exception.UserNotFoundException;
import org.ikuzo.otboo.domain.user.mapper.UserMapper;
import org.ikuzo.otboo.domain.user.repository.UserRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class OtbooUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;

    @Transactional(readOnly = true)
    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        User user = userRepository.findByEmail(email)
            .orElseThrow(() -> UserNotFoundException.withEmail(email));
        UserDto userDto = userMapper.toDto(user);

        return new OtbooUserDetails(
            userDto,
            user.getPassword()
        );
    }
}
