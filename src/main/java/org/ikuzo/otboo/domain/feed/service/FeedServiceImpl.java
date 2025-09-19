package org.ikuzo.otboo.domain.feed.service;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ikuzo.otboo.domain.clothes.entity.Clothes;
import org.ikuzo.otboo.domain.clothes.repository.ClothesRepository;
import org.ikuzo.otboo.domain.feed.dto.FeedCreateRequest;
import org.ikuzo.otboo.domain.feed.dto.FeedDto;
import org.ikuzo.otboo.domain.feed.entity.Feed;
import org.ikuzo.otboo.domain.feed.mapper.FeedMapper;
import org.ikuzo.otboo.domain.feed.repository.FeedRepository;
import org.ikuzo.otboo.domain.user.entity.User;
import org.ikuzo.otboo.domain.user.exception.UserNotFoundException;
import org.ikuzo.otboo.domain.user.repository.UserRepository;
import org.ikuzo.otboo.domain.weather.entity.Weather;
import org.ikuzo.otboo.domain.weather.exception.WeatherNotFoundException;
import org.ikuzo.otboo.domain.weather.repository.WeatherRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
@RequiredArgsConstructor
public class FeedServiceImpl implements FeedService {

    private final FeedRepository feedRepository;
    private final UserRepository userRepository;
    private final WeatherRepository weatherRepository;
    private final ClothesRepository clothesRepository;
    private final FeedMapper feedMapper;

    @Override
    @Transactional
    public FeedDto createFeed(FeedCreateRequest req) {

        User author = userRepository.findById(req.authorId())
            .orElseThrow(UserNotFoundException::new);
        Weather weather = weatherRepository.findById(req.weatherId())
            .orElseThrow(WeatherNotFoundException::new);

        // 의상 ID 중복 제거 후 일괄 조회 + 개수 검증
        Set<UUID> uniqueIds = new HashSet<>(req.clothesIds());
        List<Clothes> clothesList = clothesRepository.findAllById(uniqueIds);
        
        if (clothesList.size() != uniqueIds.size()) {
            Set<UUID> found = clothesList.stream().map(Clothes::getId).collect(Collectors.toSet());
            Set<UUID> missing = new HashSet<>(uniqueIds);
            missing.removeAll(found);
            throw new IllegalArgumentException("존재하지 않는 의상 ID: " + missing);
        }

        Feed feed = Feed.builder()
            .author(author)
            .weather(weather)
            .content(req.content())
            .likedByMe(false)
            .build();

        feed.attachClothes(clothesList);
        Feed saved = feedRepository.save(feed);

        return feedMapper.toDto(saved);
    }
}