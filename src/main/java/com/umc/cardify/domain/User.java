package com.umc.cardify.domain;

import com.umc.cardify.domain.enums.AuthProvider;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.DynamicInsert;
import org.hibernate.annotations.DynamicUpdate;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "user", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"email", "provider"})
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@DynamicUpdate
@DynamicInsert
public class User extends BaseEntity {

    @Setter()
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_id")
    private Long userId;

    @Column(name = "name", columnDefinition = "varchar(30)")
    @Setter
    private String name;

    @Column(name = "email", columnDefinition = "varchar(320)")
    private String email;

    @Enumerated(EnumType.STRING)
    @Column(name = "provider", columnDefinition = "varchar(30)")
    private AuthProvider provider;

    @Column(name = "provider_id", columnDefinition = "varchar(320)")
    private String providerId;

    @Column(name = "profile_image")
    @Setter
    private String profileImage;

    @Column(name = "notification_enabled")
    @Setter
    private boolean notificationEnabled = true;

    @Column(name = "refresh_token", length = 512)
    @Setter
    private String refreshToken;

    @Setter
    private Integer point = 5000;

    @Column(name = "today_check")
    @Setter
    private int todayCheck = 0;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL)
    private List<Folder> userFolderList = new ArrayList<>();

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL)
    private List<Download> downloadList = new ArrayList<>();

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL)
    private List<SearchHistory> searchHistoryList = new ArrayList<>();

    // Subscription 관계 추가
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL)
    private List<Subscription> subscriptions = new ArrayList<>();

    @Builder
    public User(String name, String email, AuthProvider provider, String providerId, String profileImage, Integer point, boolean notificationEnabled) {
        this.name = name;
        this.email = email;
        this.provider = provider;
        this.providerId = providerId;
        this.profileImage = profileImage;
        this.point = point;
        this.notificationEnabled = notificationEnabled;
    }
}