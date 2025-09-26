package org.ikuzo.otboo.domain.comment.service;

import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ikuzo.otboo.domain.comment.dto.CommentCreateRequest;
import org.ikuzo.otboo.domain.comment.dto.CommentDto;
import org.ikuzo.otboo.domain.comment.entity.Comment;
import org.ikuzo.otboo.domain.comment.mapper.CommentMapper;
import org.ikuzo.otboo.domain.comment.repository.CommentRepository;
import org.ikuzo.otboo.domain.feed.entity.Feed;
import org.ikuzo.otboo.domain.feed.exception.FeedNotFoundException;
import org.ikuzo.otboo.domain.feed.repository.FeedRepository;
import org.ikuzo.otboo.domain.user.entity.User;
import org.ikuzo.otboo.domain.user.exception.UserNotFoundException;
import org.ikuzo.otboo.domain.user.repository.UserRepository;
import org.ikuzo.otboo.global.dto.PageResponse;
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
    private final CommentMapper commentMapper;

    @Transactional
    @Override
    public CommentDto create(CommentCreateRequest request) {
        UUID userId = currentUserId();
        UUID feedId = request.feedId();

        log.info("[CommentService] 피드 댓글 생성 시작 authorId={} feedId={}", userId, feedId);

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

        log.info("[CommentService] 피드 댓글 생성 완료 authorId={} feedId={}", userId, feedId);

        return commentMapper.toDto(comment);
    }

    @Transactional(readOnly = true)
    @Override
    public PageResponse<CommentDto> getComments(UUID feedId,
                                                String cursor,
                                                UUID idAfter,
                                                Integer limit) {

        int pageLimit = (limit == null || limit <= 0) ? 10 : Math.min(limit, 50);

        Instant cursorInstant = parseInstant(cursor);

        List<Comment> comments = commentRepository.findCommentsWithCursor(feedId, cursorInstant, idAfter, pageLimit);

        boolean hasNext = comments.size() > pageLimit;
        List<Comment> content = hasNext ? comments.subList(0, pageLimit) : comments;

        String nextCursor = null;
        UUID nextIdAfter = null;
        if (hasNext && !content.isEmpty()) {
            Comment last = content.get(content.size() - 1);
            if (last.getCreatedAt() != null) {
                nextCursor = last.getCreatedAt().toString();
            }
            nextIdAfter = last.getId();
        }

        long totalCount = commentRepository.countByFeed_Id(feedId);
        List<CommentDto> data = content.stream().map(commentMapper::toDto).toList();

        return new PageResponse<>(
            data,
            nextCursor,
            nextIdAfter,
            hasNext,
            totalCount,
            "createdAt",
            "DESCENDING"
        );
    }

    // 로그인한 사용자 ID 가져오는 메서드
    private UUID currentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof OtbooUserDetails details)) {
            throw new AuthorizationDeniedException("인증 정보가 없습니다.");
        }
        return details.getUserDto().id();
    }

    private Instant parseInstant(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return Instant.parse(value);
        } catch (DateTimeParseException e) {
            log.warn("[CommentService] cursor 파싱 실패: {}", value, e);
            return null;
        }
    }
}
