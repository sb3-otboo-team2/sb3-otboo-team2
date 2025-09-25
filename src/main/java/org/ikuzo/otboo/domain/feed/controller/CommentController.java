package org.ikuzo.otboo.domain.feed.controller;

import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ikuzo.otboo.domain.comment.dto.CommentCreateRequest;
import org.ikuzo.otboo.domain.comment.service.CommentService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/feeds/{feedId}/comments")
@Slf4j
@RequiredArgsConstructor
public class CommentController {

    private final CommentService commentService;

    @PostMapping
    public ResponseEntity<CommentCreateRequest> create(
        @PathVariable UUID feedId,
        @RequestBody @Valid CommentCreateRequest request
    ) {

        log.info("[CommentController] 피드 댓글 생성 시작 authorId={} feedId={}", request.authorId(), request.feedId());

        commentService.create(request);

        log.info("[CommentController] 피드 댓글 생성 완료 authorId={} feedId={}", request.authorId(), request.feedId());

        return ResponseEntity.ok(request);
    }
}
