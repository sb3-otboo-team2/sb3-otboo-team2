package org.ikuzo.otboo.domain.feed.controller;


import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ikuzo.otboo.domain.feedLike.service.FeedLikeService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/feeds/{feedId}/like")
@RequiredArgsConstructor
@Slf4j
public class FeedLikeController {

    private final FeedLikeService feedLikeService;

    @PostMapping
    public ResponseEntity<Void> create(
        @PathVariable UUID feedId
    ) {
        log.info("[FeedLikeController] 피드 좋아요 생성 feedId={}", feedId);

        feedLikeService.create(feedId);

        log.info("[FeedLikeController] 피드 좋아요 완료 feedId={}", feedId);

        return ResponseEntity.noContent().build();
    }
}
