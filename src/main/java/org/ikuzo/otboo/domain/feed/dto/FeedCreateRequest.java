package org.ikuzo.otboo.domain.feed.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.UUID;
import lombok.Builder;

@Builder
public record FeedCreateRequest(
    @NotNull(message = "authorId는 필수입니다.")
    UUID authorId,

    @NotNull(message = "weatherId는 필수입니다.")
    UUID weatherId,

    @NotEmpty(message = "clothesIds는 최소 1개 이상이어야 합니다.")
    List<@NotNull UUID> clothesIds,

    @NotBlank(message = "content는 비어 있을 수 없습니다.")
    @Size(max = 100, message = "content는 최대 100자까지 가능합니다.")
    String content
) {
}
