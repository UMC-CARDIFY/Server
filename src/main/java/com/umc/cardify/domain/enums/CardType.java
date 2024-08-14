package com.umc.cardify.domain.enums;

public enum CardType {
	BLANK(0),
	WORD(1),
	MULTI(2);

	private final int value;

	CardType(int value) {
		this.value = value;
	}

	public int getValue() {
		return value;
	}

	public static CardType fromValue(int value) {
		switch (value) {
			case 0: return BLANK;
			case 1: return WORD;
			case 2: return MULTI;
			default: throw new IllegalArgumentException("Invalid CardType value: " + value);
		}
	}
}