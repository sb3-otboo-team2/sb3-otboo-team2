package org.ikuzo.otboo.global.oauth2.dto;

public interface Oauth2UserInfo {
    String getEmail();
    String getProviderId();
    String getProvider();
    String getNickname();
    String getProfileImageUrl();
}
