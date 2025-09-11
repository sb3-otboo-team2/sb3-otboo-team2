package org.ikuzo.otboo.global.dto;

import java.util.List;

public record PageResponse<T>(
    List<T> content,
    Object nextCursor,
    int size,
    boolean hasNext,
    Long totalElements
) {

}
