package com.umc.cardify.domain;

import com.umc.cardify.config.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class Folder extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long folderId;

    @OneToMany(mappedBy = "folder", cascade = CascadeType.ALL)
    private List<Note> reviewList = new ArrayList<>();
}
