package org.ikuzo.otboo.domain.comment.service;

import java.util.UUID;
import org.ikuzo.otboo.domain.comment.dto.CommentCreateRequest;
import org.ikuzo.otboo.domain.comment.dto.CommentDto;
import org.ikuzo.otboo.global.dto.PageResponse;

public interface CommentService {
    CommentDto create(CommentCreateRequest request);

    PageResponse<CommentDto> getComments(UUID feedId,
                                         String cursor,
                                         UUID idAfter,
                                         Integer limit);
}
