package com.umc.cardify.domain.enums;

import lombok.Getter;

@Getter
public enum StudyStatus {
	BEFORE_STUDY("학습 전"),
	IN_PROGRESS("학습 중"),
	PERMANENT_STORAGE("영구 보관");

	private final String description;

	StudyStatus(String description) {
		this.description = description;
	}

}
