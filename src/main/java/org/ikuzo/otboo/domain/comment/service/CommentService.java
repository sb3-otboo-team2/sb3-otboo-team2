package org.ikuzo.otboo.domain.comment.service;

import org.ikuzo.otboo.domain.comment.dto.CommentCreateRequest;

public interface CommentService {
    void create(CommentCreateRequest request);
}
