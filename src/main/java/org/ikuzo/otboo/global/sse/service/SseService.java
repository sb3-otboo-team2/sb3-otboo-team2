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
            log.debug("sse on onCompletion");
            sseEmitterRepository.delete(receiverId, sseEmitter);
        });
        sseEmitter.onTimeout(() -> {
            log.debug("sse on onTimeout");
            sseEmitterRepository.delete(receiverId, sseEmitter);
        });
        sseEmitter.onError((ex) -> {
            log.debug("sse on onError");
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
                                log.error(e.getMessage(), e);
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
        SseMessage message = sseMessageRepository.save(SseMessage.create(receiverIds, eventName, data));
        Set<ResponseBodyEmitter.DataWithMediaType> event = message.toEvent();
        sseEmitterRepository.findAllByReceiverIdsIn(receiverIds)
            .forEach(sseEmitter -> {
                try {
                    sseEmitter.send(event);
                } catch (IOException e) {
                    log.error(e.getMessage(), e);
                }
            });
    }

    public void broadcast(String eventName, Object data) {
        SseMessage message = sseMessageRepository.save(SseMessage.createBroadcast(eventName, data));
        Set<ResponseBodyEmitter.DataWithMediaType> event = message.toEvent();
        sseEmitterRepository.findAll()
            .forEach(sseEmitter -> {
                try {
                    sseEmitter.send(event);
                } catch (IOException e) {
                    log.error(e.getMessage(), e);
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
