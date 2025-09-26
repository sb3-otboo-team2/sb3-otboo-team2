package org.ikuzo.otboo.domain.feed.controller;

import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ikuzo.otboo.domain.comment.dto.CommentCreateRequest;
import org.ikuzo.otboo.domain.comment.dto.CommentDto;
import org.ikuzo.otboo.domain.comment.service.CommentService;
import org.ikuzo.otboo.global.dto.PageResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/feeds/{feedId}/comments")
@Slf4j
@RequiredArgsConstructor
public class CommentController {

    private final CommentService commentService;

    @PostMapping
    public ResponseEntity<CommentDto> create(
        @PathVariable UUID feedId,
        @RequestBody @Valid CommentCreateRequest request
    ) {

        if (!feedId.equals(request.feedId())) {
            throw new IllegalArgumentException("경로의 feedId와 요청 본문의 feedId가 일치하지 않습니다.");
        }

        log.info("[CommentController] 피드 댓글 생성 시작 authorId={} feedId={}", request.authorId(), feedId);

        CommentDto dto = commentService.create(request);

        log.info("[CommentController] 피드 댓글 생성 완료 authorId={} feedId={}", request.authorId(), feedId);

        return ResponseEntity.ok(dto);
    }

    @GetMapping
    public ResponseEntity<PageResponse<CommentDto>> getComments(
        @PathVariable UUID feedId,
        @RequestParam(required = false) String cursor,
        @RequestParam(required = false) UUID idAfter,
        @RequestParam(required = false, defaultValue = "10") Integer limit
    ) {
        log.info("[CommentController] 피드 댓글 조회 시작 feedId={}", feedId);

        PageResponse<CommentDto> response = commentService.getComments(feedId, cursor, idAfter, limit);

        log.info("[CommentController] 피드 댓글 조회 완료 feedId={}", feedId);

        return ResponseEntity.ok(response);
    }
}
