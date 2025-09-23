package org.ikuzo.otboo.global.oauth2.dto;

import java.util.Map;

public class GoogleUserInfo implements Oauth2UserInfo {

    private final Map<String, Object> attributes;

    public GoogleUserInfo(Map<String, Object> attributes) {
        this.attributes = attributes;
    }

    @Override
    public String getEmail() {
        return (String) attributes.get("email");
    }

    @Override
    public String getProviderId() {
        return (String) attributes.get("sub");
    }

    @Override
    public String getProvider() {
        return "google";
    }

    @Override
    public String getNickname() {
        return (String) attributes.get("family_name") + (String) attributes.get("given_name");
    }

    @Override
    public String getProfileImageUrl() {
        return (String) attributes.get("picture");
    }
}
