package org.ikuzo.otboo.domain.directMessage.websocket.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ikuzo.otboo.domain.directMessage.dto.DirectMessageCreateRequest;
import org.ikuzo.otboo.domain.directMessage.dto.DirectMessageDto;
import org.ikuzo.otboo.domain.directMessage.service.DirectMessageService;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Controller;

@Slf4j
@Controller
@RequiredArgsConstructor
public class WebsocketController {

    private final DirectMessageService directMessageService;

    @MessageMapping("/direct-messages_send")
    public DirectMessageDto sendMessage(@Payload DirectMessageCreateRequest directMessageCreateRequest) {
        log.info("====================================================");
        log.info("웹소켓 메시지 수신됨");
        log.info("요청 내용: {}", directMessageCreateRequest);
        log.info("senderId: {}, receiverId: {}, content: {}", 
            directMessageCreateRequest.senderId(), 
            directMessageCreateRequest.receiverId(), 
            directMessageCreateRequest.content());
        log.info("====================================================");
        
        DirectMessageDto response = directMessageService.sendMessage(directMessageCreateRequest);
        
        log.info("웹소켓 메시지 처리 완료 - 응답: {}", response);
        return response;
    }
}
