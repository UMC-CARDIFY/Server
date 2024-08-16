package com.umc.cardify.controller;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.umc.cardify.dto.card.CardRequest;
import com.umc.cardify.dto.card.CardResponse;
import com.umc.cardify.jwt.JwtUtil;
import com.umc.cardify.service.CardComponentService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@CrossOrigin(origins = "http://localhost:5173")
@Tag(name = "CardController", description = "카드 관련 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("api/v1/cards")
public class CardController {

	private final CardComponentService cardComponentService;

	private final JwtUtil jwtUtil;

	@PostMapping(value = "/add/Image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	@Operation(summary = "이미지 카드 생성", description = "이미지 및 가림판들의 크기와 위치 전송")
	public ResponseEntity<String> addImageCard(@RequestPart("image") MultipartFile image,
		@RequestPart("imageCard") CardRequest.addImageCard request) {

		String imgUrl = cardComponentService.addImageCard(image, request);

		return ResponseEntity.ok(imgUrl);
	}

	@GetMapping(value = "/view/{imgCardId}/Image")
	@Operation(summary = "이미지 카드 조회", description = "이미지 및 가림판들의 크기와 위치 조회")
	public ResponseEntity<CardResponse.getImageCard> viewImageCard(@PathVariable Long imgCardId) {

		return ResponseEntity.ok(cardComponentService.viewImageCard(imgCardId));
	}

	@PutMapping(value = "/edit/{imgCardId}/Image")
	@Operation(summary = "이미지 카드 편집", description = "이미지 및 가림판 들의 크기와 위치 조회")
	public ResponseEntity<String> editImageCard(@RequestPart("imageCard") CardRequest.addImageCard request,
		@PathVariable Long imgCardId) {

		String imgUrl = cardComponentService.editImageCard(request, imgCardId);

		return ResponseEntity.ok(imgUrl);
	}

	@GetMapping
	@Operation(summary = "플래시 카드 목록 조회(메인 화면)", description = "유저 노트 중 플래시 카드가 포함된 노트 목록 조회")
	public ResponseEntity<Page<CardResponse.getStudyCardSetLists>> viewStudyCardSetLists(
		@RequestHeader("Authorization") String token, @RequestParam(defaultValue = "0") int page,
		@RequestParam(defaultValue = "4") int size) {
		Long userId = jwtUtil.extractUserId(token);

		Pageable pageable = PageRequest.of(page, size);
		Page<CardResponse.getStudyCardSetLists> cardListsPage = cardComponentService.getStudyCardSetLists(userId,
			pageable);

		return ResponseEntity.ok(cardListsPage);
	}

	@GetMapping(value = "/{studyCardSetId}")
	@Operation(summary = "학습 카드 - 일반 학습", description = "해당 노트(StudyCardSet)의 학습 카드 전부를 Pageable 리스트로 전달")
	public ResponseEntity<Page<CardResponse.getCardLists>> normalStudy(@PathVariable Long studyCardSetId, @RequestParam(defaultValue = "0") int page) {
		Page<CardResponse.getCardLists> getCardLists = cardComponentService.getCardLists(studyCardSetId, page);

		return ResponseEntity.ok(getCardLists);
	}
}
