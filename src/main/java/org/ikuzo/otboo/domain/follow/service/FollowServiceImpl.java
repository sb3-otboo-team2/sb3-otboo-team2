package org.ikuzo.otboo.domain.follow.service;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ikuzo.otboo.domain.follow.dto.FollowCreateRequest;
import org.ikuzo.otboo.domain.follow.dto.FollowDto;
import org.ikuzo.otboo.domain.follow.dto.FollowSummaryDto;
import org.ikuzo.otboo.domain.follow.entity.Follow;
import org.ikuzo.otboo.domain.follow.exception.FollowNotFoundException;
import org.ikuzo.otboo.domain.follow.mapper.FollowMapper;
import org.ikuzo.otboo.domain.follow.repository.FollowRepository;
import org.ikuzo.otboo.domain.user.dto.UserSummary;
import org.ikuzo.otboo.domain.user.entity.User;
import org.ikuzo.otboo.domain.user.exception.UserNotFoundException;
import org.ikuzo.otboo.domain.user.repository.UserRepository;
import org.ikuzo.otboo.global.dto.PageResponse;
import org.ikuzo.otboo.domain.follow.exception.FollowAlreadyException;
import org.ikuzo.otboo.domain.follow.exception.FollowSelfNotAllowException;
import org.ikuzo.otboo.global.event.message.FollowCreatedEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.file.AccessDeniedException;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class FollowServiceImpl implements FollowService {

    private final FollowRepository followRepository;
    private final UserRepository userRepository;
    private final FollowMapper followMapper;
    private final ApplicationEventPublisher eventPublisher;


    /**
     * 팔로우 등록
     *
     * @param request
     *  - followeeId: 팔로우 대상 UserId
     *  - followerId: 팔로우 신청한 UserId
     *
     * @return FollowDto
     */
    @Override
    @Transactional
    public FollowDto follow(FollowCreateRequest request) {
        UUID followeeId = request.followeeId();
        UUID followerId = request.followerId();

        if (followeeId.equals(followerId)) {
            throw FollowSelfNotAllowException.notAllowFollowSelf(followerId);
        }

        boolean exist = followRepository.existsByFollower_IdAndFollowing_Id(followerId, followeeId);
        if (exist) {
            throw FollowAlreadyException.alreadyException(followerId);
        }

        User follower = userRepository.findById(followerId).orElseThrow(UserNotFoundException::new);
        User followee = userRepository.findById(followeeId).orElseThrow(UserNotFoundException::new);

        Follow follow = Follow.builder()
            .follower(follower)
            .following(followee)
            .build();
        Follow savedFollow = followRepository.save(follow);

        UserSummary followerSummary = new UserSummary(follower.getId(), follower.getName(), follower.getProfileImageUrl());
        UserSummary followeeSummary = new UserSummary(followee.getId(), followee.getName(), followee.getProfileImageUrl());

        FollowDto dto = followMapper.toDto(savedFollow, followeeSummary, followerSummary);

        eventPublisher.publishEvent(
            new FollowCreatedEvent(
                dto,
                Instant.now()
            )
        );
        return dto;
    }

    /**
     * 팔로우 요약 정보
     *
     * @param userId: 팔로우 대상 UserId
     * @return FollowSummaryDto
     */
    @Override
    @Transactional(readOnly = true)
    public FollowSummaryDto followSummary(UUID userId) {
        log.info("[FollowService] followSummary 팔로우 요약 정보 userId: {}", userId);
        // TODO: SpringSecurity 개발 후 securityContextHolder를 통해 접속 중인 유저 조회
//        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
//        String email = authentication.getName();
//        userRepository.findByEmail(email).orElseThrow(() -> new EntityNotFoundException("유저를 찾을 수 없습니다."));

        UUID currentUserId = UUID.randomUUID();

        long followCount = followRepository.countByFollowing_Id(userId);
        long followingCount = followRepository.countByFollower_Id(userId);

        // 내가 이 유저를 팔로우하는지 여부 + 팔로우 ID
        Optional<Follow> myFollow = followRepository.findByFollower_IdAndFollowing_Id(currentUserId, userId);
        boolean followedByMe = myFollow.isPresent();
        UUID followedByMeId = myFollow.map(Follow::getId).orElse(null);

        // 대상 사용자가 나를 팔로우하는지 여부
        boolean followingMe = followRepository.existsByFollower_IdAndFollowing_Id(userId, currentUserId);

        return new FollowSummaryDto(
            userId,
            followCount,
            followingCount,
            followedByMe,
            followedByMeId,
            followingMe
        );
    }
    
    /**
     * 팔로워 목록 조회
     *
     * @param followeeId: 조회할 UserId
     * @param cursor: 커서 (2025-09-10T09:47:14.318813Z)
     * @param idAfter: 보조 커서 (0f6a481b-aee8-41ce-8aaf-2cf76434b395)
     * @param limit: 사이즈
     * @param nameLike: 이름 검색 필터
     * @return PageResponse<FollowDto>
     */
    @Override
    @Transactional(readOnly = true)
    public PageResponse<FollowDto> getFollowers(UUID followeeId, String cursor, UUID idAfter, int limit, String nameLike) {
        log.info("[FollowService] 팔로워 목록 조회 서비스 진입");
        List<Follow> followers = followRepository.getFollows(followeeId, cursor, idAfter, limit, nameLike, "follower");

        List<Follow> followList = followers.size() > limit ? followers.subList(0, limit) : followers;

        boolean hasNext = followers.size() > limit;

        Instant nextCursor = null;
        UUID nextIdAfter = null;

        if (hasNext && !followList.isEmpty()) {
            Follow last = followList.get(followList.size() - 1);
            nextCursor = last.getCreatedAt();
            nextIdAfter = last.getId();
        }
        String sortBy = "createdAt";
        String sortDirection = "DESCENDING";
        long totalCount = followRepository.countByCursorFilter(followeeId, nameLike, "follower");

        List<FollowDto> content = followList.stream()
            .map(follow -> {
                UserSummary follower = new UserSummary(follow.getFollower().getId(), follow.getFollower().getName(), follow.getFollower().getProfileImageUrl());
                UserSummary followee = new UserSummary(follow.getFollowing().getId(), follow.getFollowing().getName(), follow.getFollowing().getProfileImageUrl());
                return followMapper.toDto(follow, followee, follower);
            })
            .toList();

        return new PageResponse<>(
            content,
            nextCursor,
            nextIdAfter,
            hasNext,
            totalCount,
            sortBy,
            sortDirection
        );
    }

    /**
     * 팔로잉 목록 조회
     *
     * @param followeeId: 조회할 UserId
     * @param cursor: 커서 (2025-09-10T09:47:14.318813Z)
     * @param idAfter: 보조 커서 (0f6a481b-aee8-41ce-8aaf-2cf76434b395)
     * @param limit: 사이즈
     * @param nameLike: 이름 검색 필터
     * @return PageResponse<FollowDto>
     */
    @Override
    @Transactional(readOnly = true)
    public PageResponse<FollowDto> getFollowings(UUID followeeId, String cursor, UUID idAfter, int limit, String nameLike) {
        log.info("[FollowService] 팔로잉 목록 조회 서비스 진입");
        List<Follow> followings = followRepository.getFollows(followeeId, cursor, idAfter, limit, nameLike, "following");

        List<Follow> followList = followings.size() > limit ? followings.subList(0, limit) : followings;
        boolean hasNext = followings.size() > limit;
        Instant nextCursor = null;
        UUID nextIdAfter = null;
        if (hasNext && !followList.isEmpty()) {
            Follow last = followList.get(followList.size() - 1);
            nextCursor = last.getCreatedAt();
            nextIdAfter = last.getId();
        }
        String sortBy = "createdAt";
        String sortDirection = "DESCENDING";
        long totalCount = followRepository.countByCursorFilter(followeeId, nameLike, "following");

        List<FollowDto> content = followList.stream()
            .map(follow -> {
                UserSummary follower = new UserSummary(follow.getFollower().getId(), follow.getFollower().getName(), follow.getFollower().getProfileImageUrl());
                UserSummary followee = new UserSummary(follow.getFollowing().getId(), follow.getFollowing().getName(), follow.getFollowing().getProfileImageUrl());
                return followMapper.toDto(follow, followee, follower);
            })
            .toList();

        return new PageResponse<>(
            content,
            nextCursor,
            nextIdAfter,
            hasNext,
            totalCount,
            sortBy,
            sortDirection
        );
    }

    @Override
    @Transactional
    public void cancel(UUID followId) {
        Follow follow = followRepository.findById(followId).orElseThrow(() -> FollowNotFoundException.notFoundException(followId));

        // TODO: SpringSecurity 적용 후 로그인한 유저와 follow.getFollower 검증 리팩토링 예정
//        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
//        String email = authentication.getName();
//        User user = userRepository.findByEmail(email).orElseThrow(() -> new EntityNotFoundException("유저를 찾을 수 없습니다."));
//
//        if (!follow.getFollower().getId().equals(user.getId())) {
//            throw new AuthorizationDeniedException("팔로우를 취소할 권한이 없습니다.");
//        }

        followRepository.delete(follow);
        log.info("[FollowService] 팔로우 취소 완료");
    }
}
