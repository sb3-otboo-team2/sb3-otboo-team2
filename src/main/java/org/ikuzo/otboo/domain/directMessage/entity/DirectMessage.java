package org.ikuzo.otboo.domain.directMessage.entity;

import jakarta.persistence.*;
import lombok.*;
import org.ikuzo.otboo.domain.user.entity.User;
import org.ikuzo.otboo.global.base.BaseEntity;

@Entity
@Table(name = "direct_messages")
@Getter
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class DirectMessage extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sender", nullable = false)
    private User sender;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "receiver", nullable = false)
    private User receiver;

    @Column(name = "content", length = 300)
    private String content;

}
