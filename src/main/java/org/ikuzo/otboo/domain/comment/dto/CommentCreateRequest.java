package org.ikuzo.otboo.domain.comment.dto;

import java.util.UUID;

public record CommentCreateRequest(
    UUID feedId,
    UUID authorId,
    String content
) {
}
