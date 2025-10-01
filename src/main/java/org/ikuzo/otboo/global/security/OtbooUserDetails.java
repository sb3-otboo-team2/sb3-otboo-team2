package org.ikuzo.otboo.global.security;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import org.ikuzo.otboo.domain.user.dto.UserDto;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.core.user.OAuth2User;

@EqualsAndHashCode(of = "userDto")
@Getter
public class OtbooUserDetails implements UserDetails, OAuth2User {

    private final UserDto userDto;
    private final String password;
    private Map<String, Object> attributes;

    public OtbooUserDetails(UserDto userDto, String password) {
        this.userDto = userDto;
        this.password = password;
    }

    public OtbooUserDetails(UserDto userDto, String password, Map<String, Object> attributes) {
        this.userDto = userDto;
        this.password = password;
        this.attributes = attributes;
    }

    @Override
    public Map<String, Object> getAttributes() {
        return attributes;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + userDto.role().name()));
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String getUsername() {
        return userDto.email();
    }

    @Override
    public String getName() {
        return userDto.email();
    }

    @Override
    public boolean isAccountNonLocked() {
        return !userDto.locked();
    }
}
