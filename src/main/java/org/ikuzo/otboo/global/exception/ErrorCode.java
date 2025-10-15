package org.ikuzo.otboo.global.exception;

import lombok.Getter;

@Getter
public enum ErrorCode {

    // Security
    INVALID_TOKEN("토큰이 유효하지 않습니다."),
    INVALID_USER_DETAILS("사용자 인증 정보(UserDetails)가 유효하지 않습니다."),

    // 사용자
    DUPLICATE_USER("이미 존재하는 사용자입니다."),
    USER_NOT_FOUND("사용자를 찾을 수 없습니다."),
    USER_LOCATION_MISSING("사용자 위도/경도 정보가 없습니다."),

    // NULL 검증
    REQUIRED_FIELD_MISSING("필수 필드 값이 누락되었습니다"),

    // 의상
    CLOTHES_NOT_FOUND("존재하지 않는 의상입니다"),
    CLOTHING_MAPPER_CONVERSION_FAILED("Clothes -> ClothesDto로의 변환에 실패하였습니다"),


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

    // 피드
    FEED_NOT_FOUND("해당 피드는 존재하지 않습니다."),
    FEED_CLOTHES_NOT_FOUND("존재하지 않는 의상 ID가 포함되어 있습니다."),
    FEED_UNMATCH_AUTHOR("해당 사용자가 게시한 피드가 아닙니다."),
    FEED_UNMATCH_CLOTHES_OWNER("해당 사용자가 가지고 있지 않는 의상입니다."),
    FEED_LIKE_ALREADY_EXISTS("이미 좋아요를 누른 피드입니다."),
    FEED_LIKE_NOT_FOUND("삭제할 좋아요가 존재하지 않습니다"),

    // 날씨/외부 API
    WEATHER_NOT_FOUND("날씨값을 찾을 수 없습니다."),
    WEATHER_NO_FORECAST("기상청 예보 데이터가 없습니다."),
    EXTERNAL_API_ERROR("외부 API 호출 중 오류가 발생했습니다."),

    // 알림
    NOTIFICATION_NOT_FOUND("존재하지 않는 알림 입니다."),

    // Server 에러
    INTERNAL_SERVER_ERROR("서버 내부 오류가 발생했습니다."),

    // Kafka
    KAFKA_SERIALIZATION_ERROR("카프카 직렬화/역직렬화에 실패했습니다."),
    KAFKA_PUBLISH_ERROR("카프카 전송에 실패했습니다."),
    KAFKA_INFRA_ERROR("카프카 인프라 오류가 발생했습니다.");


    private final String message;

    ErrorCode(String message) {
        this.message = message;
    }
} 
