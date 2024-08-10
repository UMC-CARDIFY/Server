package com.umc.cardify.dto.card;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Getter;

public class CardRequest {

	@Getter
	@Schema(title = "CARD_REQ_01 : 이미지 카드 생성 요청 DTO")
	public static class addImageCard{
		@NotBlank(message = "이미지 너비 입력 필요")
		@Schema(description = "이미지 너비", example = "800")
		Long baseImageWidth;

		@NotBlank(message = "이미지 높이 입력 필요")
		@Schema(description = "이미지 높이", example = "600")
		Long baseImageHeight;

		@Schema(description = "가림판 배열")
		List<addImageCardOverlay> overlays;
	}

	@Getter
	@Schema(title = "CARD_REQ_02 : 이미지 카드 생성 요청 내부 가림판 정보 DTO")
	@Builder
	public static class addImageCardOverlay{
		@NotBlank(message = "가림판 x 좌표 입력 필요")
		@Schema(description = "가림판 x좌표", example = "400")
		private Long positionOfX;

		@NotBlank(message = "가림판 y 좌표 입력 필요")
		@Schema(description = "가림판 y좌표", example = "500")
		private Long positionOfY;

		@NotBlank(message = "가림판 너비 입력 필요")
		@Schema(description = "가림판 너비", example = "100")
		private Long width;


		@NotBlank(message = "가림판 높이 입력 필요")
		@Schema(description = "가림판 높이", example = "50")
		private Long height;
	}
	@Getter
	@Schema(title = "CARD_REQ_03 : 노트 작성시 카드 양식 DTO")
	public static class WriteCardDto{
		@NotNull
		String name;
		@NotNull
		String text;
	}
}
