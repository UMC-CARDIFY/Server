package com.umc.cardify.dto.card;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

public class CardResponse {
	@Getter
	@Schema(title = "CARD_REQ_01 : 이미지 카드 조회 DTO")
	@Builder
	public static class getImageCard {
		@Schema(description = "배경 이미지")
		String imgUrl;

		@Schema(description = "이미지 너비", example = "800")
		Long baseImageWidth;

		@Schema(description = "이미지 높이", example = "600")
		Long baseImageHeight;

		@Schema(description = "가림판 배열")
		List<CardRequest.addImageCardOverlay> overlays;
	}
}
