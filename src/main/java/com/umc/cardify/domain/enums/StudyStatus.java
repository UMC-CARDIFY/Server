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

	// Method to get the integer representation
	public int getValue() {
		return this.ordinal();
	}

	// Static method to get enum from integer value
	public static StudyStatus fromValue(int value) {
		for (StudyStatus status : values()) {
			if (status.ordinal() == value) {
				return status;
			}
		}
		throw new IllegalArgumentException("Invalid StudyStatus value: " + value);
	}
}
