package org.ikuzo.otboo.global.event.kafka;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Stream;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ikuzo.otboo.domain.follow.repository.FollowRepository;
import org.ikuzo.otboo.domain.notification.entity.Level;
import org.ikuzo.otboo.domain.notification.service.NotificationService;
import org.ikuzo.otboo.domain.user.repository.UserRepository;
import org.ikuzo.otboo.global.event.message.ClothesAttributeDefCreatedEvent;
import org.ikuzo.otboo.global.event.message.FeedCreatedEvent;
import org.ikuzo.otboo.global.event.message.FeedLikeCreatedEvent;
import org.ikuzo.otboo.global.event.message.FollowCreatedEvent;
import org.ikuzo.otboo.global.event.message.MessageCreatedEvent;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@RequiredArgsConstructor
@Component
public class NotificationRequiredTopicListener {

    private final NotificationService notificationService;
    private final ObjectMapper objectMapper;
    private final FollowRepository followRepository;
    private final UserRepository userRepository;

    @KafkaListener(topics = "otboo.FollowCreatedEvent")
    public void onFollowCreatedEvent(String kafkaEvent) {
        try {
            log.info("Kafka 메시지 수신 (팔로우): {}", kafkaEvent);
            FollowCreatedEvent event = objectMapper.readValue(kafkaEvent, FollowCreatedEvent.class);

            UUID receiverId = event.getDto().followee().userId();
            String followerName = event.getDto().follower().name();

            String title = "\"" + followerName + "\"님이 나를 팔로우했어요.";
            String content = "";

            log.info("팔로우 알림 생성 - receiverId: {}, followerName: {}", receiverId, followerName);
            notificationService.create(Set.of(receiverId), title, content, Level.INFO);
            log.info("팔로우 알림 생성 완료 - receiverId: {}", receiverId);
        } catch (JsonProcessingException e) {
            log.error("Kafka 메시지 역직렬화 실패 (팔로우): {}", kafkaEvent, e);
        } catch (Exception e) {
            log.error("팔로우 알림 생성 실패: {}", kafkaEvent, e);
        }
    }

    // MessageCreatedEvent는 WebsocketRequiredTopicListener에서만 처리
    // DM 알림은 DirectMessage 저장 시 직접 생성하거나, 별도 이벤트로 처리해야 함

    @KafkaListener(topics = "otboo.FeedLikeCreatedEvent")
    public void onFeedLikeCreatedEvent(String kafkaEvent) {
        try {
            log.info("Kafka 메시지 수신 (좋아요): {}", kafkaEvent);
            FeedLikeCreatedEvent event = objectMapper.readValue(kafkaEvent, FeedLikeCreatedEvent.class);

            UUID receiverId = event.getDto().author().userId();
            String likerName = event.getDto().liker().name();
            UUID likerId = event.getDto().liker().userId();

            // 자기 자신의 피드에 좋아요를 누른 경우 알림을 보내지 않음
            if (receiverId.equals(likerId)) {
                log.info("자기 자신의 좋아요이므로 알림 생성 생략 - userId: {}", receiverId);
                return;
            }

            String title = likerName + " 님이 내 피드에 좋아요를 눌렀습니다.";
            String content = event.getDto().feedContent();

            log.info("좋아요 알림 생성 - receiverId: {}, likerName: {}", receiverId, likerName);
            notificationService.create(Set.of(receiverId), title, content, Level.INFO);
            log.info("좋아요 알림 생성 완료 - receiverId: {}", receiverId);
        } catch (JsonProcessingException e) {
            log.error("Kafka 메시지 역직렬화 실패 (좋아요): {}", kafkaEvent, e);
        } catch (Exception e) {
            log.error("좋아요 알림 생성 실패: {}", kafkaEvent, e);
        }
    }

    @KafkaListener(topics = "otboo.FeedCreatedEvent")
    public void onFeedCreatedEvent(String kafkaEvent) {
        try {
            log.info("Kafka 메시지 수신 (피드 생성): {}", kafkaEvent);
            FeedCreatedEvent event = objectMapper.readValue(kafkaEvent, FeedCreatedEvent.class);

            UUID authorId = event.getDto().author().userId();
            Set<UUID> followerIds = Set.copyOf(followRepository.findFollowerIdsByFollowingId(authorId));
            
            if (followerIds.isEmpty()) {
                log.info("팔로워가 없어 피드 생성 알림 생략 - authorId: {}", authorId);
                return;
            }

            String title = event.getDto().author().name() + " 님이 새 피드를 등록했습니다.";
            String content = event.getDto().content();

            log.info("피드 생성 알림 생성 - authorId: {}, 팔로워 수: {}", authorId, followerIds.size());
            notificationService.create(followerIds, title, content, Level.INFO);
            log.info("피드 생성 알림 생성 완료 - 팔로워 수: {}", followerIds.size());
        } catch (JsonProcessingException e) {
            log.error("Kafka 메시지 역직렬화 실패 (피드 생성): {}", kafkaEvent, e);
        } catch (Exception e) {
            log.error("피드 생성 알림 생성 실패: {}", kafkaEvent, e);
        }
    }

    @KafkaListener(topics = "otboo.ClothesAttributeDefCreatedEvent")
    @Transactional
    public void onClothesAttributeDefCreatedEvent(String kafkaEvent) {
        try {
            log.info("Kafka 메시지 수신 (의상 속성 추가): {}", kafkaEvent);
            ClothesAttributeDefCreatedEvent event = objectMapper.readValue(kafkaEvent,
                ClothesAttributeDefCreatedEvent.class);

            String title = "새로운 의상 속성이 추가되었습니다";
            String content = "내 의상에 [" + event.getDto().name() + "]을 추가해보세요.";

            final int BATCH_SIZE = 1000;
            int totalUsers = 0;
            int batchCount = 0;
            
            try (Stream<UUID> userIdStream = userRepository.streamUserIdsByLockedFalse()) {
                Set<UUID> batch = new HashSet<>();
                userIdStream.forEach(userId -> {
                    batch.add(userId);
                    if (batch.size() >= BATCH_SIZE) {
                        notificationService.create(Set.copyOf(batch), title, content, Level.INFO);
                        batch.clear();
                    }
                });

                if (!batch.isEmpty()) {
                    totalUsers += batch.size();
                    batchCount++;
                    notificationService.create(batch, title, content, Level.INFO);
                }
            }
            
            log.info("의상 속성 추가 알림 생성 완료 - 총 사용자 수: {}, 배치 수: {}", totalUsers, batchCount);
        } catch (JsonProcessingException e) {
            log.error("Kafka 메시지 역직렬화 실패 (의상 속성 추가): {}", kafkaEvent, e);
        } catch (Exception e) {
            log.error("의상 속성 추가 알림 생성 실패: {}", kafkaEvent, e);
        }
    }
}
