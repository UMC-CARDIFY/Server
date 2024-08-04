package com.umc.cardify.domain;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
public class User extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_id")
    private Long userId;

    @Column(name = "name", columnDefinition = "varchar(30)")
    private String name;

//    @Column(name = "url_profile", columnDefinition = "text")
//    private String urlProfile;Re

    @Column(name = "email", columnDefinition = "varchar(320)")
    private String email;

    @Column(name = "password", columnDefinition = "varchar(255) NOT NULL")
    private String password;

    @Column(name = "kakao", columnDefinition = "boolean")
    private boolean kakao;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL)
    private List<Folder> userFolderList = new ArrayList<>();

    @Builder
    public User(String name, String email, String password, boolean kakao) {
        this.name = name;
        this.email = email;
        this.password = password;
        this.kakao = kakao;
    }

    public User (String name, String email, boolean kakao) {
        this.name = name;
        this.email = email;
        this.password = "KakaoPassw0rd"; // 카카오라 의미 없음
        this.kakao = kakao;
    }

}
