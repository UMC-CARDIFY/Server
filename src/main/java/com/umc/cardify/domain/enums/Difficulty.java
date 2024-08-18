package com.umc.cardify.domain.enums;

import lombok.Getter;

@Getter
public enum Difficulty {
    NONE(0), HARD(1), NORMAL(2), EASY(3), PASS(4);

    private final int value;

    Difficulty(int value) {
        this.value = value;
    }

    public static Difficulty fromValue(int value) {
		return switch (value) {
			case 0 -> NONE;
			case 1 -> HARD;
			case 2 -> NORMAL;
			case 3 -> EASY;
			case 4 -> PASS;
			default -> throw new IllegalArgumentException("Unknown difficulty value: " + value);
		};
    }
}

