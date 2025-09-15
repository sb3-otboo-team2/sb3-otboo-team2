package org.ikuzo.otboo.global.exception;

import lombok.Getter;

@Getter
public enum ErrorCode {
    // 사용자
    DUPLICATE_USER("이미 존재하는 사용자입니다."),

    // 의상 속성
    DUPLICATED_ATTRIBUTE_NAME("이미 존재하는 속성 이름입니다"),

    // 팔로우
    FOLLOW_SELF_NOT_ALLOWED("자기 자신은 팔로우 할 수 없습니다."),
    FOLLOW_ALREADY_EXISTS("이미 팔로우한 사용자 입니다.");

    private final String message;

    ErrorCode(String message) {
        this.message = message;
    }
} 