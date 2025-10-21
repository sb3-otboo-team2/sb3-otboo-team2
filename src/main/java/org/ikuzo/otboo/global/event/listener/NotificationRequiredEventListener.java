package org.ikuzo.otboo.global.event.listener;

import lombok.RequiredArgsConstructor;
import org.ikuzo.otboo.domain.notification.entity.Level;
import org.ikuzo.otboo.domain.notification.service.NotificationService;
import org.ikuzo.otboo.global.event.message.FollowCreatedEvent;
import org.ikuzo.otboo.global.event.message.MessageCreatedEvent;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.Set;
import java.util.UUID;

//@Component
@RequiredArgsConstructor
public class NotificationRequiredEventListener {

    private final NotificationService notificationService;

    @TransactionalEventListener
    public void on(FollowCreatedEvent event) {
        UUID receiverId = event.getDto().followee().userId();
        String followerName = event.getDto().follower().name();

        String title = "\"" + followerName + "\"님이 나를 팔로우했어요.";

        String content = "";

        notificationService.create(Set.of(receiverId), title, content, Level.INFO);
    }

    @TransactionalEventListener
    public void on(MessageCreatedEvent event) {
        UUID receiverId = event.getDto().receiver().userId();
        String senderName = event.getDto().sender().name();

        String title = "\"" + senderName + "\"님이 메세지를 보냈어요.";

        String content = event.getDto().content();

        notificationService.create(Set.of(receiverId), title, content, Level.INFO);
    }
}
