package org.ikuzo.otboo.domain.directMessage.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.ikuzo.otboo.global.base.BaseEntity;

@Entity
@Table(name = "direct_messages")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class DirectMessage extends BaseEntity {

    @Column(name = "sender")
    private User sender;

    @Column(name = "receiver")
    private User receiver;

    @Column(name = "content", length = 300)
    private String content;

}
