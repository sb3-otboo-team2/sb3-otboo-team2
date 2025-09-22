package org.ikuzo.otboo.global.oauth2.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ikuzo.otboo.domain.user.dto.UserDto;
import org.ikuzo.otboo.domain.user.entity.User;
import org.ikuzo.otboo.domain.user.mapper.UserMapper;
import org.ikuzo.otboo.domain.user.repository.UserRepository;
import org.ikuzo.otboo.global.oauth2.dto.KakaoUserInfo;
import org.ikuzo.otboo.global.oauth2.dto.Oauth2UserInfo;
import org.ikuzo.otboo.global.security.OtbooUserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class Oauth2UserServiceImpl extends DefaultOAuth2UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserMapper userMapper;

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oAuth2User = super.loadUser(userRequest);

        Oauth2UserInfo oauth2UserInfo = null;

        if (userRequest.getClientRegistration().getRegistrationId().equals("kakao")) {
            oauth2UserInfo = new KakaoUserInfo(oAuth2User.getAttributes());
        }

        String email = oauth2UserInfo.getEmail();
        String nickname = oauth2UserInfo.getNickname();
        String profileImageUrl = oauth2UserInfo.getProfileImageUrl();
        String providerId = oauth2UserInfo.getProviderId();
        String provider = oauth2UserInfo.getProvider();

        Optional<User> findUser = userRepository.findByEmail(email);

        UserDto userDto = null;
        if (findUser.isEmpty()) {
            log.info("[Oauth2UserService] Kakao 최초 로그인");

            User user = new User(
                email,
                nickname,
                passwordEncoder.encode(email),
                profileImageUrl,
                provider,
                providerId
            );
            User saved = userRepository.save(user);
            userDto = userMapper.toDto(saved);
            return new OtbooUserDetails(userDto, saved.getPassword(), oAuth2User.getAttributes());
        } else {
            User user = findUser.get();
            userDto = userMapper.toDto(user);
            return new OtbooUserDetails(userDto, user.getPassword(), oAuth2User.getAttributes());
        }
    }
}
