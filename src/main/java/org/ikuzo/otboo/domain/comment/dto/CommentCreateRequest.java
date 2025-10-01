package org.ikuzo.otboo.domain.comment.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.UUID;

public record CommentCreateRequest(
    @NotNull UUID feedId,
    @NotNull UUID authorId,
    @NotBlank @Size(max = 100) String content
) {
}
