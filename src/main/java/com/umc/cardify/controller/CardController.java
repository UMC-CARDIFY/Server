package com.umc.cardify.controller;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import com.umc.cardify.dto.card.CardRequest;
import com.umc.cardify.dto.card.CardResponse;
import com.umc.cardify.jwt.JwtUtil;
import com.umc.cardify.service.CardService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@CrossOrigin(origins = "http://localhost:5173")
@Tag(name = "CardController", description = "카드 관련 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("api/v1/cards")
public class CardController {

	private final JwtUtil jwtUtil;

	private final CardService cardService;

	@PostMapping(value = "/add/Image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	@Operation(summary = "이미지 카드 생성", description = "이미지 및 가림판들의 크기와 위치 전송")
	public ResponseEntity<String> addImageCard(@RequestPart("image") MultipartFile image,
		@RequestPart("imageCard") CardRequest.addImageCard request) {

		String imgUrl = cardService.addImageCard(image, request);

		return ResponseEntity.ok(imgUrl);
	}

	@GetMapping(value = "/view/{imgCardId}/Image")
	@Operation(summary = "이미지 카드 조회", description = "이미지 및 가림판 들의 크기와 위치 조회")
	public ResponseEntity<CardResponse.getImageCard> viewImageCard(@PathVariable Long imgCardId) {

		return ResponseEntity.ok(cardService.viewImageCard(imgCardId));
	}
	@PutMapping(value = "/edit/{imgCardId}/Image")
	@Operation(summary = "이미지 카드 편집", description = "이미지 및 가림판 들의 크기와 위치 조회")
	public ResponseEntity<String> editImageCard(@RequestPart("imageCard") CardRequest.addImageCard request,
		@PathVariable Long imgCardId) {

		String imgUrl = cardService.editImageCard(request, imgCardId);

		return ResponseEntity.ok(imgUrl);
	}
}
