package org.ikuzo.otboo.domain.notification.entity;

import jakarta.persistence.*;
import lombok.*;
import org.ikuzo.otboo.domain.user.entity.User;
import org.ikuzo.otboo.global.base.BaseEntity;

import java.util.UUID;

@Entity
@Table(name = "notifications")
@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Builder
public class Notification extends BaseEntity {


    @Column(name = "receiver_id")
    private UUID receiverId;

    @Column(name = "title")
    private String title;

    @Column(name = "content")
    private String content;

    @Column(name = "level")
    @Enumerated(EnumType.STRING)
    private Level level;
}
