package org.ikuzo.otboo.domain.user.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.ikuzo.otboo.global.base.BaseUpdatableEntity;

@Entity
@Table(name = "users")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User extends BaseUpdatableEntity {

    @Column(length = 100, nullable = false, unique = true)
    private String email;

    @Column(length = 20, nullable = false)
    private String name;

    @Column(length = 60, nullable = false)
    private String password;

    @Enumerated(EnumType.STRING)
    private Gender gender;

    @Column
    private Instant birthDate;

    @Column
    private Double latitude;

    @Column
    private Double longitude;

    @Column
    private Integer x;

    @Column
    private Integer y;

    @Column(length = 255)
    private String locationNames;

    @Column(nullable = false)
    private int temperatureSensitivity;

    @Column(columnDefinition = "TEXT")
    private String profileImageUrl;

    @Column(nullable = false)
    private boolean locked;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;
}