package org.ikuzo.otboo.domain.follow.service;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ikuzo.otboo.domain.follow.dto.FollowCreateRequest;
import org.ikuzo.otboo.domain.follow.dto.FollowDto;
import org.ikuzo.otboo.domain.follow.dto.FollowSummaryDto;
import org.ikuzo.otboo.domain.follow.entity.Follow;
import org.ikuzo.otboo.domain.follow.mapper.FollowMapper;
import org.ikuzo.otboo.domain.follow.repository.FollowRepository;
import org.ikuzo.otboo.domain.user.dto.UserSummary;
import org.ikuzo.otboo.domain.user.entity.User;
import org.ikuzo.otboo.domain.user.repository.UserRepository;
import org.ikuzo.otboo.global.exception.follow.FollowAlreadyException;
import org.ikuzo.otboo.global.exception.follow.FollowSelfNotAllowException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class FollowServiceImpl implements FollowService {

    private final FollowRepository followRepository;
    private final UserRepository userRepository;
    private final FollowMapper followMapper;


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

        User follower = userRepository.findById(followerId).orElseThrow(() -> new EntityNotFoundException("팔로워를 찾을 수 없음"));
        User followee = userRepository.findById(followeeId).orElseThrow(() -> new EntityNotFoundException("팔로이를 찾을 수 없음"));

        Follow follow = Follow.builder()
            .follower(follower)
            .following(followee)
            .build();
        Follow savedFollow = followRepository.save(follow);

        UserSummary followerSummary = new UserSummary(follower.getId(), follower.getName(), follower.getProfileImageUrl());
        UserSummary followeeSummary = new UserSummary(followee.getId(), followee.getName(), followee.getProfileImageUrl());

        return followMapper.toDto(savedFollow, followerSummary, followeeSummary);
    }

    /**
     * 팔로우 요약 정보
     *
     * @param userId: 팔로우 대상 UserId
     * @return FollowSummaryDto
     */
    @Override
    public FollowSummaryDto followSummary(UUID userId) {
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
}
