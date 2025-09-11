package org.ikuzo.otboo.domain.feed.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.ikuzo.otboo.global.base.BaseUpdatableEntity;

@Entity
@Table(name = "feed_clothes")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FeedClothes extends BaseUpdatableEntity {


    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "feed_id", nullable = false)
    private Feed feed;

//    @ManyToOne(fetch = FetchType.LAZY)
//    @JoinColumn(name = "clothes_id", nullable = false)
//    private Clothes clothes;

}