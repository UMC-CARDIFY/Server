package com.umc.cardify.domain;

import com.umc.cardify.domain.enums.AuthProvider;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.DynamicInsert;
import org.hibernate.annotations.DynamicUpdate;

import java.util.ArrayList;
import java.util.List;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@DynamicUpdate
@DynamicInsert
@Builder
public class User extends BaseEntity {

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
    @Builder.Default
    @Setter
    private boolean notificationEnabled = true;

    @Column(name = "refresh_token", length = 512)
    @Setter
    private String refreshToken;

    @Builder.Default
    @Setter
    private Integer point = 5000;  // 초기값 설정

    @Column(name = "subscribe")
    @Builder.Default
    private boolean subscribe = false ; // 구독제 여부

    // 연령대 추가할지

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL)
    private List<Folder> userFolderList = new ArrayList<>();

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL)
    private List<Download> downloadList = new ArrayList<>();

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL)
    private List<SearchHistory> searchHistoryList = new ArrayList<>();

    @Column(name = "today_check")
    @Setter
    private int todayCheck = 0;

}