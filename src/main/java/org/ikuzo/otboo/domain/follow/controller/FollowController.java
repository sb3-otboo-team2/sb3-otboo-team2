package org.ikuzo.otboo.domain.follow.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ikuzo.otboo.domain.follow.dto.FollowCreateRequest;
import org.ikuzo.otboo.domain.follow.dto.FollowDto;
import org.ikuzo.otboo.domain.follow.dto.FollowSummaryDto;
import org.ikuzo.otboo.domain.follow.service.FollowService;
import org.ikuzo.otboo.global.dto.PageResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RequiredArgsConstructor
@Slf4j
@RestController
@RequestMapping("/api/follows")
public class FollowController {

    private final FollowService followService;

    @PostMapping
    public ResponseEntity<FollowDto> follow(@RequestBody FollowCreateRequest request) {
        FollowDto response = followService.follow(request);
        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    @GetMapping("/summary")
    public ResponseEntity<FollowSummaryDto> summary(@RequestParam UUID userId) {
        log.info("[FollowController] 팔로우 요약 정보 컨트롤러 진입 userId: {}", userId);
        FollowSummaryDto response = followService.followSummary(userId);
        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    @GetMapping("/followers")
    public ResponseEntity<PageResponse<FollowDto>> followers(
        @RequestParam UUID followeeId,
        @RequestParam(required = false) String cursor,
        @RequestParam(required = false) UUID idAfter,
        @RequestParam int limit,
        @RequestParam(required = false) String nameLike
    ) {
        log.info("[FollowController] 팔로워 목록 조회 컨트롤러 진입 followeeId: {}, cursor: {}, idAfter: {}, limit: {}, nameLike: {}", followeeId, cursor, idAfter,  limit, nameLike);
        PageResponse<FollowDto> response = followService.getFollowers(followeeId, cursor, idAfter, limit, nameLike);
        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    @GetMapping("/followings")
    public ResponseEntity<PageResponse<FollowDto>> followings(
        @RequestParam UUID followeeId,
        @RequestParam(required = false) String cursor,
        @RequestParam(required = false) UUID idAfter,
        @RequestParam int limit,
        @RequestParam(required = false) String nameLike
    ) {
        log.info("[FollowController] 팔로잉 목록 조회 컨트롤러 진입 followeeId: {}, cursor: {}, idAfter: {}, limit: {}, nameLike: {}", followeeId, cursor, idAfter,  limit, nameLike);
        PageResponse<FollowDto> response = followService.getFollowings(followeeId, cursor, idAfter, limit, nameLike);
        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    @DeleteMapping("/{followId}")
    public ResponseEntity<Void> cancelFollow(@PathVariable UUID followId) {
        log.info("[FollowController] 팔로우 취소 컨트롤러 진입 followId: {}", followId);
        followService.cancel(followId);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }
}
