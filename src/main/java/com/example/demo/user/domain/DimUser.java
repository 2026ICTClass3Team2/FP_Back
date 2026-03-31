package com.example.demo.user.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.DynamicInsert;

import java.time.LocalDate;

@Entity
@Table(name = "dim_user")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@DynamicInsert
public class DimUser {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "dim_user_id")
    private Long id;

    @Column(nullable = false, length = 50)
    private String nickname;

    @Column(name = "user_id", nullable = false, unique = true, length = 50)
    private String userId;

    @Column(nullable = false, unique = true, length = 100)
    private String email;

    @Column(name = "total_points")
    @ColumnDefault("0")
    private Integer totalPoints;

    @Column(name = "`rank`") // DB 예약어 충돌 방지
    @ColumnDefault("1")
    private Integer rank;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 1)
    @ColumnDefault("'u'")
    private Role role; // Enum 클래스 (u, a)

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    @ColumnDefault("'bronze'")
    private Tier tier; // Enum 클래스 (bronze, silver 등)

    @CreationTimestamp
    @Column(name = "registered_at", updatable = false)
    private LocalDate registeredAt;

    @Builder
    public DimUser(String nickname, String userId, String email) {
        this.nickname = nickname;
        this.userId = userId;
        this.email = email;
    }

    //    비즈니스 로직 메서드



    public void addPoints(int points) {
        this.totalPoints += points;
    }

    public void updateTier(Tier newTier) {
        this.tier = newTier;
    }
}
