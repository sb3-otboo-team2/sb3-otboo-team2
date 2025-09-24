package org.ikuzo.otboo.domain.feed.repository.dto;

public enum FeedSortKey {
    CREATED_AT,
    LIKE_COUNT;

    public static FeedSortKey from(String value) {
        if (value == null) {
            return CREATED_AT;
        }
        if ("likeCount".equalsIgnoreCase(value.trim())) {
            return LIKE_COUNT;
        }
        return CREATED_AT;
    }
}
