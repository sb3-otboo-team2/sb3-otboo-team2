package org.ikuzo.otboo.domain.feed.controller;


import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.ikuzo.otboo.domain.feedLike.service.FeedLikeService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/feeds/{feedId}/like")
@RequiredArgsConstructor
public class FeedLikeController {

    private final FeedLikeService feedLikeService;

    @PostMapping
    public ResponseEntity<Void> create(
        @PathVariable UUID feedId
    ) {
        feedLikeService.create(feedId);
        return ResponseEntity.noContent().build();
    }
}
