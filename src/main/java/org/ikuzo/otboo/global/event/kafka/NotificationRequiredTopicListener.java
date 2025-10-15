package org.ikuzo.otboo.global.event.kafka;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ikuzo.otboo.domain.follow.repository.FollowRepository;
import org.ikuzo.otboo.domain.notification.entity.Level;
import org.ikuzo.otboo.domain.notification.service.NotificationService;
import org.ikuzo.otboo.domain.user.repository.UserRepository;
import org.ikuzo.otboo.global.base.BaseEntity;
import org.ikuzo.otboo.global.event.message.ClothesAttributeDefCreatedEvent;
import org.ikuzo.otboo.global.event.message.FeedCreatedEvent;
import org.ikuzo.otboo.global.event.message.FeedLikeCreatedEvent;
import org.ikuzo.otboo.global.event.message.FollowCreatedEvent;
import org.ikuzo.otboo.global.event.message.MessageCreatedEvent;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

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
            FollowCreatedEvent event = objectMapper.readValue(kafkaEvent, FollowCreatedEvent.class);

            UUID receiverId = event.getDto().followee().userId();
            String followerName = event.getDto().follower().name();

            String title = "\"" + followerName + "\"님이 나를 팔로우했어요.";

            String content = "";

            notificationService.create(Set.of(receiverId), title, content, Level.INFO);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    @KafkaListener(topics = "otboo.MessageCreatedEvent")
    public void onMessageCreatedEvent(String kafkaEvent) {
        try {
            MessageCreatedEvent event = objectMapper.readValue(kafkaEvent,
                MessageCreatedEvent.class);

            UUID receiverId = event.getDto().receiver().userId();
            String senderName = event.getDto().sender().name();

            String title = "\"" + senderName + "\"님이 메세지를 보냈어요.";

            String content = event.getDto().content();

            notificationService.create(Set.of(receiverId), title, content, Level.INFO);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    @KafkaListener(topics = "otboo.FeedLikeCreatedEvent")
    public void onFeedLikeCreatedEvent(String kafkaEvent) {
        try {
            FeedLikeCreatedEvent event = objectMapper.readValue(kafkaEvent,
                FeedLikeCreatedEvent.class);

            UUID receiverId = event.getDto().author().userId();
            String likerName = event.getDto().liker().name();
            UUID likerId = event.getDto().liker().userId();

            // 자기 자신의 피드에 좋아요를 누른 경우 알림을 보내지 않음
            if (receiverId.equals(likerId)) {
                return;
            }

            String title = likerName + " 님이 내 피드에 좋아요를 눌렀습니다.";
            String content = event.getDto().feedContent();

            notificationService.create(Set.of(receiverId), title, content, Level.INFO);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    @KafkaListener(topics = "otboo.FeedCreatedEvent")
    public void onFeedCreatedEvent(String kafkaEvent) {
        try {
            FeedCreatedEvent event = objectMapper.readValue(kafkaEvent, FeedCreatedEvent.class);

            UUID authorId = event.getDto().author().userId();
            Set<UUID> followerIds = Set.copyOf(
                followRepository.findFollowerIdsByFollowingId(authorId));
            if (followerIds.isEmpty()) {
                return;
            }

            String title = event.getDto().author().name() + " 님이 새 피드를 등록했습니다.";
            String content = event.getDto().content();

            notificationService.create(followerIds, title, content, Level.INFO);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    @KafkaListener(topics = "otboo.ClothesAttributeDefCreatedEvent")
    public void onClothesAttributeDefCreatedEvent(String kafkaEvent) {
        try {
            ClothesAttributeDefCreatedEvent event = objectMapper.readValue(kafkaEvent,
                ClothesAttributeDefCreatedEvent.class);

            Set<UUID> allUserIds = Set.copyOf(userRepository.findUserIdsByLockedFalse());

            if (allUserIds.isEmpty()) {
                return;
            }

            String title = "새로운 의상 속성이 추가되었습니다";
            String content = "내 의상에 [" + event.getDto().name() + "]을 추가해보세요.";

            notificationService.create(allUserIds, title, content, Level.INFO);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }
}
