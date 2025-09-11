package org.ikuzo.otboo.domain.follow.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.ikuzo.otboo.global.base.BaseEntity;

@Entity
@Table(name = "follows")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Follow extends BaseEntity {

    @Column(name = "follower_id")
    private User follower;

    @Column(name = "following_id")
    private User following;

}
