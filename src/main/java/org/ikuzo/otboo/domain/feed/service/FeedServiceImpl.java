package org.ikuzo.otboo.domain.feed.service;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
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
import org.ikuzo.otboo.domain.feed.exception.FeedClothesNotFoundException;
import org.ikuzo.otboo.domain.feed.exception.FeedClothesUnmatchOwner;
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

        // 의상 ID 중복 제거 후 입력 순서 유지하며 조회 + 검증
        Set<UUID> uniqueIds = new LinkedHashSet<>(req.clothesIds());
        List<Clothes> clothesList = clothesRepository.findAllById(uniqueIds);

        Map<UUID, Clothes> clothesById = clothesList.stream()
            .collect(Collectors.toMap(
                Clothes::getId,
                c -> c,
                (left, right) -> left,
                LinkedHashMap::new));

        if (clothesById.size() != uniqueIds.size()) {
            Set<UUID> missing = new LinkedHashSet<>(uniqueIds);
            missing.removeAll(clothesById.keySet());
            throw FeedClothesNotFoundException.withMissingIds(missing);
        }

        Set<UUID> unauthorized = uniqueIds.stream()
            .filter(id -> {
                Clothes clothes = clothesById.get(id);
                return clothes == null
                    || clothes.getOwner() == null
                    || !author.getId().equals(clothes.getOwner().getId());
            })
            .collect(Collectors.toCollection(LinkedHashSet::new));

        if (!unauthorized.isEmpty()) {
            throw FeedClothesUnmatchOwner.withUnauthorizedIds(unauthorized);
        }

        List<Clothes> orderedClothes = uniqueIds.stream()
            .map(clothesById::get)
            .toList();

        Feed feed = Feed.builder()
            .author(author)
            .weather(weather)
            .content(req.content())
            .likedByMe(false)
            .build();

        feed.attachClothes(orderedClothes);
        Feed saved = feedRepository.save(feed);

        return feedMapper.toDto(saved);
    }
}
