package com.umc.cardify.domain;

import com.umc.cardify.domain.enums.MarkStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UpdateTimestamp;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class Folder extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "folder_id")
    private Long folderId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @Column(name = "name")
    private String name;

    @Column(name = "color")
    private String color;

    @Getter
    @Enumerated(EnumType.STRING)
    @Column(name = "mark_state", columnDefinition = "VARCHAR(15) DEFAULT 'INACTIVE'")
    private MarkStatus markState;

    @Column(name = "mark_date")
    private Timestamp markDate;

    @UpdateTimestamp
    private Timestamp editDate;

    @OneToMany(mappedBy = "folder", cascade = CascadeType.ALL)
    private List<Note> notes;

    @OneToMany(mappedBy = "folder", cascade = CascadeType.ALL)
    private List<Note> reviewList = new ArrayList<>();

    @Transient
    public int getNoteCount() {
        return notes != null ? notes.size() : 0;
    }

    // 하위폴더 - Folder 엔티티 자체로 재귀적 관계 설정
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_folder_id")
    private Folder parentFolder;

    @OneToMany(mappedBy = "parentFolder", cascade = CascadeType.ALL)
    private List<Folder> subFolders = new ArrayList<>();
}
