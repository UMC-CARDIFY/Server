package com.umc.cardify.domain;

import java.time.LocalDateTime;

import org.hibernate.annotations.DynamicInsert;
import org.hibernate.annotations.DynamicUpdate;

import com.umc.cardify.domain.enums.StudyStatus;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@DynamicUpdate
@DynamicInsert
public class StudyCardSet extends BaseEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "study_card_set_id")
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "note_id")
	private Note note;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "folder_id")
	private Folder folder;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "user_id")
	private User user;

	@Column(name = "study_status", nullable = false)
	@Enumerated(EnumType.ORDINAL)
	private StudyStatus studyStatus;

	@Column(name = "note_name", nullable = false)
	private String noteName;

	@Column(name = "color")
	private String color;

	@Column(name = "recent_study_date")
	private LocalDateTime recentStudyDate;

	@Column(name = "next_study_date")
	private LocalDateTime nextStudyDate;

	// 편의를 위해 엔티티 내부에서 정보 설정 메서드를 추가할 수 있음
	public void setNoteInfo(Note note) {
		this.note = note;
		this.noteName = note.getName();
	}

	public void setFolderInfo(Folder folder) {
		this.folder = folder;
		this.color = folder.getColor();
	}
}