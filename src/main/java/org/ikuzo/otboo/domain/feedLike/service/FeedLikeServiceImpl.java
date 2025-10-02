package org.ikuzo.otboo.domain.feedLike.service;

import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ikuzo.otboo.domain.feed.entity.Feed;
import org.ikuzo.otboo.domain.feed.exception.FeedLikeNotFoundException;
import org.ikuzo.otboo.domain.feed.exception.FeedNotFoundException;
import org.ikuzo.otboo.domain.feed.repository.FeedRepository;
import org.ikuzo.otboo.domain.feedLike.entity.FeedLike;
import org.ikuzo.otboo.domain.feedLike.repository.FeedLikeRepository;
import org.ikuzo.otboo.domain.notification.entity.Level;
import org.ikuzo.otboo.domain.notification.service.NotificationService;
import org.ikuzo.otboo.domain.user.entity.User;
import org.ikuzo.otboo.domain.user.exception.UserNotFoundException;
import org.ikuzo.otboo.domain.user.repository.UserRepository;
import org.ikuzo.otboo.global.security.OtbooUserDetails;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
@RequiredArgsConstructor
public class FeedLikeServiceImpl implements FeedLikeService {
    private final FeedRepository feedRepository;
    private final UserRepository userRepository;
    private final FeedLikeRepository feedLikeRepository;
    private final NotificationService notificationService;

    @Override
    @Transactional
    public void create(UUID feedId) {
        log.info("[FeedLikeService] 피드 좋아요 생성 시작 feedId={}", feedId);

        UUID userId = currentUserId();

        User user = userRepository.findById(userId)
            .orElseThrow(() -> UserNotFoundException.withId(userId));

        Feed feed = feedRepository.findById(feedId)
            .orElseThrow(() -> new FeedNotFoundException(feedId));
        feed.like();

        FeedLike feedLike = FeedLike.builder()
            .user(user)
            .feed(feed)
            .build();

        feedLikeRepository.save(feedLike);

        User author = feed.getAuthor();
        if (author != null && !author.getId().equals(userId)) {
            String title = user.getName() + " 님이 내 피드에 좋아요를 눌렀습니다.";
            String content = feed.getContent();
            notificationService.create(Set.of(author.getId()), title, content, Level.INFO);
        }

        log.info("[FeedLikeService] 피드 좋아요 생성 완료 feedId={}, userId={}", feedId, userId);
    }

    @Override
    @Transactional
    public void delete(UUID feedId) {
        log.info("[FeedLikeService] 피드 좋아요 삭제 시작 feedId={}", feedId);

        UUID userId = currentUserId();

        User user = userRepository.findById(userId)
            .orElseThrow(() -> UserNotFoundException.withId(userId));

        Feed feed = feedRepository.findById(feedId)
            .orElseThrow(() -> new FeedNotFoundException(feedId));
        feed.unlike();

        FeedLike feedLike = feedLikeRepository.findByFeedAndUser(feed, user)
            .orElseThrow(FeedLikeNotFoundException::new);

        feedLikeRepository.delete(feedLike);

        log.info("[FeedLikeService] 피드 좋아요 삭제 완료 feedId={}, userId={}", feedId, userId);
    }


    private UUID currentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof OtbooUserDetails details)) {
            throw new AuthorizationDeniedException("인증 정보가 없습니다.");
        }
        return details.getUserDto().id();
    }
}
