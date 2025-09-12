package org.ikuzo.otboo.global.dto;

import java.util.List;
import java.util.UUID;

public record PageResponse<T>(
    List<T> content,
    Object nextCursor,
    UUID nextIdAfter,
    boolean hasNext,
    Long totalCount,
    String sortBy,
    String sortDirection
) {

}
