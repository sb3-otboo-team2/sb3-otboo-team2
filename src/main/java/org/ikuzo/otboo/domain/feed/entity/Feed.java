package org.ikuzo.otboo.domain.feed.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.util.ArrayList;
import java.util.List;
import org.ikuzo.otboo.domain.comment.entity.Comment;
import org.ikuzo.otboo.domain.feedclothes.entity.FeedClothes;
import org.ikuzo.otboo.domain.feedlike.entity.FeedLike;
import org.ikuzo.otboo.domain.weather.entity.Weather;
import org.ikuzo.otboo.global.base.BaseUpdatableEntity;

@Entity
@Table(name = "feeds")
public class Feed extends BaseUpdatableEntity {

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false)
    private String content;

    @ManyToOne
    @JoinColumn(name = "weather_id", nullable = false)
    private Weather weather;

    @OneToMany(mappedBy = "feed", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<FeedClothes> feedClothes = new ArrayList<>();

    @OneToMany(mappedBy = "feed", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<FeedLike> feedLikes = new ArrayList<>();

    @OneToMany(mappedBy = "feed", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Comment> comments = new ArrayList<>();

}