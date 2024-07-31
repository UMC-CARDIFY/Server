package com.umc.cardify.domain;

import com.umc.cardify.config.BaseEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.ArrayList;
import java.util.Collection;
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

    @Column(name = "name", columnDefinition = "varchar(20) NOT NULL")
    private String name;

//    @Column(name = "url_profile", columnDefinition = "text")
//    private String urlProfile;

    @Column(name = "email", columnDefinition = "varchar(320)")
    private String email;

    @Column(name = "pawssword", columnDefinition = "varchar(255) NOT NULL")
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
