package org.ikuzo.otboo.global.exception;

import lombok.Getter;

@Getter
public enum ErrorCode {
    // 사용자
    DUPLICATE_USER("이미 존재하는 사용자입니다."),
    USER_LOCATION_MISSING("사용자 위도/경도 정보가 없습니다."),

    // NULL 검증
    REQUIRED_FIELD_MISSING("필수 필드 값이 누락되었습니다"),

    // 의상
    CLOTHES_NOT_FOUND("존재하지 않는 의상입니다"),

    // 의상 속성
    DUPLICATED_ATTRIBUTE_NAME("이미 존재하는 속성 이름입니다"),
    ATTRIBUTE_NOT_FOUND("존재하지 않는 속성 입니다"),

    // 속성 옵션
    INVALID_ATTRIBUTE_OPTION("해당 속성에 존재하지 않는 옵션 값입니다"),
    OPTION_VALUE_NOT_FOUND("존재하지 않는 옵션입니다"),

    // 팔로우
    FOLLOW_SELF_NOT_ALLOWED("자기 자신은 팔로우 할 수 없습니다."),
    FOLLOW_ALREADY_EXISTS("이미 팔로우한 사용자 입니다."),
    FOLLOW_NOT_FOUND("존재하지 않는 팔로우 입니다."),

    // 날씨/외부 API
    WEATHER_NO_FORECAST("기상청 예보 데이터가 없습니다."),
    EXTERNAL_API_ERROR("외부 API 호출 중 오류가 발생했습니다.");

    private final String message;

    ErrorCode(String message) {
        this.message = message;
    }
} 