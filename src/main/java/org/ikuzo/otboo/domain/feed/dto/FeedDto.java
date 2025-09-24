package org.ikuzo.otboo.domain.feed.dto;


import java.time.Instant;
import java.util.List;
import java.util.UUID;
import lombok.Builder;
import org.ikuzo.otboo.domain.weather.dto.WeatherDto;

@Builder(toBuilder = true)
public record FeedDto(
    UUID id,
    Instant createdAt,
    Instant updatedAt,
    AuthorDto author,
    WeatherDto weather,
    List<OotdDto> ootds,
    String content,
    long likeCount,
    long commentCount,
    boolean likedByMe
) {

    // Entity의 세터메서드 지우고 DTO 에서 LikedByMe 주입
    public FeedDto withLikedByMe(boolean likedByMe) {
        return this.toBuilder().likedByMe(likedByMe).build();
    }
}
