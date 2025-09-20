package org.ikuzo.otboo.domain.feed.mapper;

import org.ikuzo.otboo.domain.clothes.mapper.ClothesMapper;
import org.ikuzo.otboo.domain.feed.dto.FeedDto;
import org.ikuzo.otboo.domain.feed.entity.Feed;
import org.ikuzo.otboo.domain.user.mapper.UserMapper;
import org.ikuzo.otboo.domain.weather.mapper.WeatherMapper;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring", uses = {UserMapper.class, WeatherMapper.class, ClothesMapper.class})
public interface FeedMapper {

    @Mapping(target = "author", source = "author", qualifiedByName = "authorDto")
    @Mapping(target = "weather", source = "weather", qualifiedByName = "feedWeatherDto")
    @Mapping(target = "ootds", source = "feedClothes", qualifiedByName = "ootdList")
    @Mapping(target = "likedByMe", ignore = true)
    FeedDto toDto(Feed feed);
}
