package org.ikuzo.otboo.domain.feed.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.ikuzo.otboo.domain.feed.dto.FeedCreateRequest;
import org.ikuzo.otboo.domain.feed.dto.FeedDto;
import org.ikuzo.otboo.domain.feed.dto.FeedUpdateRequest;
import org.ikuzo.otboo.domain.feed.service.FeedService;
import org.ikuzo.otboo.global.dto.PageResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/feeds")
@RequiredArgsConstructor
public class FeedController {

    private final FeedService feedService;

    @PostMapping
    public ResponseEntity<FeedDto> create(@Valid @RequestBody FeedCreateRequest request) {
        return ResponseEntity.ok(feedService.createFeed(request));
    }

    @GetMapping
    public ResponseEntity<PageResponse<FeedDto>> getFeeds(
        @RequestParam(required = false) String cursor,
        @RequestParam(required = false) UUID idAfter,
        @RequestParam(required = false, defaultValue = "10") Integer limit,
        @RequestParam(required = false, defaultValue = "createdAt") String sortBy,
        @RequestParam(required = false, defaultValue = "DESCENDING") String sortDirection,
        @RequestParam(required = false) String keywordLike,
        @RequestParam(required = false) String skyStatusEqual,
        @RequestParam(required = false) String precipitationTypeEqual
    ) {
        PageResponse<FeedDto> response = feedService.getFeeds(cursor, idAfter, limit, sortBy, sortDirection,
            keywordLike, skyStatusEqual, precipitationTypeEqual);
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{feedId}")
    public ResponseEntity<FeedDto> update(
        @NotNull @PathVariable UUID feedId,
        @Valid @RequestBody FeedUpdateRequest request
    ) {
        return ResponseEntity.ok(feedService.updateFeed(feedId, request));
    }

    @DeleteMapping("/{feedId}")
    public ResponseEntity<Void> delete(
        @NotNull @PathVariable UUID feedId
    ) {
        feedService.deleteFeed(feedId);
        return ResponseEntity.noContent().build();
    }
}
