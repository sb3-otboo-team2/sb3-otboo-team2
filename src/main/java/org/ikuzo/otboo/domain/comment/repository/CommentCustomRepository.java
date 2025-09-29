package org.ikuzo.otboo.domain.comment.repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.ikuzo.otboo.domain.comment.entity.Comment;

public interface CommentCustomRepository {

    List<Comment> findCommentsWithCursor(UUID feedId, Instant cursor, UUID idAfter, int limit);
}