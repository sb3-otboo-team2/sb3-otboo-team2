package org.ikuzo.otboo.domain.follow.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ikuzo.otboo.domain.follow.dto.FollowCreateRequest;
import org.ikuzo.otboo.domain.follow.dto.FollowDto;
import org.ikuzo.otboo.domain.follow.dto.FollowSummaryDto;
import org.ikuzo.otboo.domain.follow.service.FollowService;
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
        FollowSummaryDto response = followService.followSummary(userId);
        return ResponseEntity.status(HttpStatus.OK).body(response);
    }
}
