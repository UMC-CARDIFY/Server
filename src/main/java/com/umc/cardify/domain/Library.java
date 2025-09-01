package com.umc.cardify.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Table(name = "note_library")
public class Library extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "library_id")
    private Long libraryId;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "note_id")
    private Note note;

    private LocalDateTime uploadAt;

    @OneToMany(mappedBy = "library", cascade = CascadeType.ALL)
    private List<LibraryCategory> categoryList = new ArrayList<>();

    @OneToMany(mappedBy = "library", cascade = CascadeType.ALL)
    private List<Download> downloadList = new ArrayList<>();
}
