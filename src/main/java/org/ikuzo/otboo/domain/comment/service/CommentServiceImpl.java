package org.ikuzo.otboo.domain.comment.service;

import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ikuzo.otboo.domain.comment.dto.CommentCreateRequest;
import org.ikuzo.otboo.domain.comment.entity.Comment;
import org.ikuzo.otboo.domain.comment.repository.CommentRepository;
import org.ikuzo.otboo.domain.feed.entity.Feed;
import org.ikuzo.otboo.domain.feed.exception.FeedNotFoundException;
import org.ikuzo.otboo.domain.feed.repository.FeedRepository;
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
public class CommentServiceImpl implements CommentService {

    private final FeedRepository feedRepository;
    private final UserRepository userRepository;
    private final CommentRepository commentRepository;

    @Transactional
    @Override
    public void create(CommentCreateRequest request) {
        log.info("[CommentService] 피드 댓글 생성 시작 authorId={} feedId={}", request.authorId(), request.feedId());

        UUID userId = currentUserId();
        UUID feedId = request.feedId();

        User user = userRepository.findById(userId)
            .orElseThrow(() -> UserNotFoundException.withId(userId));

        Feed feed = feedRepository.findById(feedId)
            .orElseThrow(() -> new FeedNotFoundException(feedId));

        Comment comment = Comment.builder()
            .author(user)
            .feed(feed)
            .content(request.content())
            .build();
        feed.increaseCommentCount();

        commentRepository.save(comment);

        log.info("[CommentService] 피드 댓글 생성 완료 authorId={} feedId={}", request.authorId(), request.feedId());
    }

    // 로그인한 사용자 ID 가져오는 메서드
    private UUID currentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof OtbooUserDetails details)) {
            throw new AuthorizationDeniedException("인증 정보가 없습니다.");
        }
        return details.getUserDto().id();
    }
}
