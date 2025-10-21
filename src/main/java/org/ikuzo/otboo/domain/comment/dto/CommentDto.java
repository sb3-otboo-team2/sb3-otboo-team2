package org.ikuzo.otboo.domain.comment.dto;

import java.time.Instant;
import java.util.UUID;
import lombok.Builder;
import org.ikuzo.otboo.domain.feed.dto.AuthorDto;

@Builder(toBuilder = true)
public record CommentDto(
    UUID id,
    Instant createdAt,
    UUID feedId,
    AuthorDto author,
    String content
) {
}
