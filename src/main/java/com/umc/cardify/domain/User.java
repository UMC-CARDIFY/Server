package com.umc.cardify.domain;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.DynamicInsert;
import org.hibernate.annotations.DynamicUpdate;
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
@DynamicUpdate
@DynamicInsert
public class User extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_id")
    private Long userId;

    @Column(name = "name", columnDefinition = "varchar(20) NOT NULL")
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL)
    private List<Folder> userFolderList = new ArrayList<>();

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

    @Builder
    public User(String name, String email, String password, boolean kakao) {
        this.name = name;
        this.email = email;
        this.password = password;
        this.kakao = kakao;
    }

    @ColumnDefault("5000")
    private Integer point;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL)
    private List<Download> downloadList = new ArrayList<>();
}
