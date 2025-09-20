package org.ikuzo.otboo.domain.feed.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.ikuzo.otboo.domain.clothes.entity.Clothes;
import org.ikuzo.otboo.global.base.BaseEntity;

@Entity
@Table(
    name = "feed_clothes",
    uniqueConstraints = @UniqueConstraint(name = "uk_feed_clothes", columnNames = {"feed_id", "clothes_id"})
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class FeedClothes extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "feed_id", nullable = false)
    private Feed feed;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "clothes_id", nullable = false)
    private Clothes clothes;


    private FeedClothes(Feed feed, Clothes clothes) {
        this.feed = feed;
        this.clothes = clothes;
    }

    public static FeedClothes of(Feed feed, Clothes clothes) {
        return new FeedClothes(feed, clothes);
    }
}