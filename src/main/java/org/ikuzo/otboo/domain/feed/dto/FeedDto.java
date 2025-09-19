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

}