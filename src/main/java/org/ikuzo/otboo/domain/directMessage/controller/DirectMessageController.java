package org.ikuzo.otboo.domain.directMessage.controller;

import lombok.RequiredArgsConstructor;
import org.ikuzo.otboo.domain.directMessage.dto.DirectMessageDto;
import org.ikuzo.otboo.domain.directMessage.service.DirectMessageService;
import org.ikuzo.otboo.global.dto.PageResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.UUID;

@RestController
@RequestMapping("/api/direct-messages")
@RequiredArgsConstructor
public class DirectMessageController {

    private final DirectMessageService directMessageService;

    @GetMapping
    public ResponseEntity<PageResponse<DirectMessageDto>> getDirectMessages(
        @RequestParam UUID userId,
        @RequestParam(required = false) Instant cursor,
        @RequestParam(required = false) UUID idAfter,
        @RequestParam int limit
        ) {
        PageResponse<DirectMessageDto> response = directMessageService.getDirectMessages(userId, cursor, idAfter, limit);
        return ResponseEntity.status(HttpStatus.OK).body(response);
    }
}
