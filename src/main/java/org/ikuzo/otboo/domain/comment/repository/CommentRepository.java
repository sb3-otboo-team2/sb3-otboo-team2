package org.ikuzo.otboo.domain.comment.repository;

import java.util.UUID;
import org.ikuzo.otboo.domain.comment.entity.Comment;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CommentRepository extends JpaRepository<Comment, UUID> {
}