package com.umc.cardify.domain.enums;

import lombok.Getter;

@Getter
public enum Difficulty {
    NONE(0), EXPERT(1), HARD(2), NORMAL(3), EASY(4);

    private final int value;

    Difficulty(int value) {
        this.value = value;
    }

    public static Difficulty fromValue(int value) {
		return switch (value) {
			case 0 -> NONE;
			case 1 -> EXPERT;
			case 2 -> HARD;
			case 3 -> NORMAL;
			case 4 -> EASY;
			default -> throw new IllegalArgumentException("Unknown difficulty value: " + value);
		};
    }
}

