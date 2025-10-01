package org.ikuzo.otboo.global.security;


import lombok.RequiredArgsConstructor;
import org.ikuzo.otboo.domain.user.dto.UserDto;
import org.ikuzo.otboo.domain.user.entity.User;
import org.ikuzo.otboo.domain.user.mapper.UserMapper;
import org.ikuzo.otboo.domain.user.repository.UserRepository;
import org.springframework.security.authentication.LockedException;
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
            .orElseThrow(() -> new UsernameNotFoundException("Invalid credentials"));

        if (user.getLocked()) {
            throw new LockedException("계정이 잠겨있습니다. 관리자에게 문의하세요.");
        }

        UserDto userDto = userMapper.toDto(user);

        return new OtbooUserDetails(
            userDto,
            user.getPassword()
        );
    }
}
