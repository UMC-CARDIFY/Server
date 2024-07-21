package com.umc.cardify.domain;

import com.umc.cardify.config.BaseEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Entity
public class User extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_id")
    private Long userId;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL)
    private List<Folder> userFolderList = new ArrayList<>();

    @Column(name = "name", columnDefinition = "varchar(30) NOT NULL")
    private String name;

//    @Column(name = "url_profile", columnDefinition = "text")
//    private String urlProfile;

    @Column(name = "email", columnDefinition = "varchar(320)")
    private String email;

    @Column(name = "pawssword", columnDefinition = "varchar(20) NOT NULL")
    private String password;

    @Column(name = "kakao", columnDefinition = "boolean")
    private boolean kakao;

    @Builder
    public User(String name, String email, String password, boolean kakao) {
        this.name = name;
        this.email = email;
        this.password = password;
        this.kakao = kakao;
    }
}
