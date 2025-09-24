package org.ikuzo.otboo.domain.user.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.time.LocalDate;
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
    private LocalDate birthDate;

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
    private Integer temperatureSensitivity = 3;

    @Column(columnDefinition = "TEXT")
    private String profileImageUrl;

    @Column(nullable = false)
    private Boolean locked = false;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role = Role.USER;

    @Column
    private String provider;

    @Column
    private String providerId;

    public User(String email, String name, String password) {
        this.email = email;
        this.name = name;
        this.password = password;
    }

    public User(String email, String name, String password, String profileImageUrl, String provider, String providerId) {
        this.email = email;
        this.name = name;
        this.password = password;
        this.profileImageUrl = profileImageUrl;
        this.provider = provider;
        this.providerId = providerId;
    }
}
