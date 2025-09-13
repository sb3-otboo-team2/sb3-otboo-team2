package org.ikuzo.otboo.global.exception;

import lombok.Getter;

@Getter
public enum ErrorCode {

    // NULL 검증
    REQUIRED_FIELD_MISSING("필수 필드 값이 누락되었습니다"),

    // 의상 속성
    DUPLICATED_ATTRIBUTE_NAME("이미 존재하는 속성 이름입니다"),
    ATTRIBUTE_NOT_FOUND("존재하지 않는 속성 입니다"),

    // 속성 옵션
    INVALID_ATTRIBUTE_OPTION("해당 속성에 존재하지 않는 옵션 값입니다"),

    // 팔로우
    FOLLOW_SELF_NOT_ALLOWED("자기 자신은 팔로우 할 수 없습니다."),
    FOLLOW_ALREADY_EXISTS("이미 팔로우한 사용자 입니다.");

    private final String message;

    ErrorCode(String message) {
        this.message = message;
    }
} 