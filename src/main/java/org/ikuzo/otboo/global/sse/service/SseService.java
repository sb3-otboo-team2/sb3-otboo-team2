package org.ikuzo.otboo.global.sse.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ikuzo.otboo.global.sse.entity.SseMessage;
import org.ikuzo.otboo.global.sse.repository.SseEmitterRepository;
import org.ikuzo.otboo.global.sse.repository.SseMessageRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyEmitter;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Collection;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class SseService {

    @Value("${sse.timeout}")
    private long timeout;

    private final SseEmitterRepository sseEmitterRepository;
    private final SseMessageRepository sseMessageRepository;

    public SseEmitter connect(UUID receiverId, UUID lastEventId) {
        SseEmitter sseEmitter = new SseEmitter(timeout);

        sseEmitter.onCompletion(() -> {
            log.info("SSE 연결 정상 종료 - receiverId: {}", receiverId);
            sseEmitterRepository.delete(receiverId, sseEmitter);
        });
        sseEmitter.onTimeout(() -> {
            log.info("SSE 연결 타임아웃 - receiverId: {}", receiverId);
            sseEmitterRepository.delete(receiverId, sseEmitter);
        });
        sseEmitter.onError((ex) -> {
            log.info("SSE 연결 에러 발생 - receiverId: {}, 에러: {}", receiverId, ex.getMessage());
            sseEmitterRepository.delete(receiverId, sseEmitter);
        });

        sseEmitterRepository.save(receiverId, sseEmitter);

        Optional.ofNullable(lastEventId)
            .ifPresentOrElse(
                id -> {
                    sseMessageRepository.findAllByEventIdAfterAndReceiverId(id, receiverId)
                        .forEach(sseMessage -> {
                            try {
                                sseEmitter.send(sseMessage.toEvent());
                            } catch (IOException e) {
                                log.warn("SSE 재연결 시 이전 메시지 전송 실패 - receiverId: {}, 에러: {}", 
                                    receiverId, e.getMessage());
                            }
                        });
                },
                () -> {
                    ping(sseEmitter);
                }
            );

        return sseEmitter;
    }

    public void send(Collection<UUID> receiverIds, String eventName, Object data) {
        log.info("SSE 메시지 전송 시작 - receiverIds: {}, eventName: {}", receiverIds, eventName);
        
        SseMessage message = sseMessageRepository.save(SseMessage.create(receiverIds, eventName, data));
        log.debug("SSE 메시지 저장 완료 - eventId: {}", message.getEventId());
        
        Set<ResponseBodyEmitter.DataWithMediaType> event = message.toEvent();
        Collection<SseEmitter> emitters = sseEmitterRepository.findAllByReceiverIdsIn(receiverIds);
        
        log.info("SSE Emitter 조회 완료 - receiverIds: {}, emitter 개수: {}", receiverIds, emitters.size());
        
        if (emitters.isEmpty()) {
            log.warn("SSE Emitter가 없습니다 - receiverIds: {}", receiverIds);
        }
        
        int successCount = 0;
        int failCount = 0;
        
        for (SseEmitter sseEmitter : emitters) {
            try {
                sseEmitter.send(event);
                successCount++;
                log.debug("SSE 메시지 전송 성공 - eventId: {}", message.getEventId());
            } catch (IOException e) {
                failCount++;
                // Broken pipe 등 연결이 끊긴 Emitter는 제거
                log.warn("SSE 연결이 끊긴 Emitter 발견 - eventId: {}, 에러: {}", 
                    message.getEventId(), e.getMessage());
                try {
                    sseEmitter.completeWithError(e);
                } catch (Exception ex) {
                    log.debug("Emitter completeWithError 실패 (이미 종료됨): {}", ex.getMessage());
                }
            }
        }
        
        log.info("SSE 메시지 전송 완료 - receiverIds: {}, 성공: {}, 실패: {}", receiverIds, successCount, failCount);
    }

    public void broadcast(String eventName, Object data) {
        SseMessage message = sseMessageRepository.save(SseMessage.createBroadcast(eventName, data));
        Set<ResponseBodyEmitter.DataWithMediaType> event = message.toEvent();
        sseEmitterRepository.findAll()
            .forEach(sseEmitter -> {
                try {
                    sseEmitter.send(event);
                } catch (IOException e) {
                    log.warn("SSE 브로드캐스트 전송 실패 - 연결 끊김: {}", e.getMessage());
                    try {
                        sseEmitter.completeWithError(e);
                    } catch (Exception ex) {
                        log.debug("Emitter completeWithError 실패 (이미 종료됨): {}", ex.getMessage());
                    }
                }
            });
    }

    @Scheduled(fixedDelay = 1000 * 60 * 30)
    public void cleanUp() {
        sseEmitterRepository.findAll()
            .stream().filter(sseEmitter -> !ping(sseEmitter))
            .forEach(
                sseEmitter -> sseEmitter.completeWithError(new RuntimeException("sse ping failed")));
    }

    private boolean ping(SseEmitter sseEmitter) {
        try {
            sseEmitter.send(SseEmitter.event()
                .name("ping")
                .build());
            return true;
        } catch (IOException e) {
            log.error("Failed to send ping event", e);
            return false;
        }
    }
}
