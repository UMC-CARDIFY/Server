package com.umc.cardify.service;

import java.util.List;
import java.util.stream.Collectors;

import com.umc.cardify.domain.Card;
import com.umc.cardify.domain.Note;
import com.umc.cardify.dto.card.CardResponse;
import com.umc.cardify.repository.CardRepository;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.umc.cardify.domain.ImageCard;
import com.umc.cardify.domain.Overlay;
import com.umc.cardify.dto.card.CardRequest;
import com.umc.cardify.repository.ImageCardRepository;
import com.umc.cardify.repository.OverlayRepository;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class CardService {

	private final S3Service s3Service;

	private final ImageCardRepository imageCardRepository;

	private final OverlayRepository overlayRepository;

	private final CardRepository cardRepository;

	@Transactional
	public String addImageCard(MultipartFile image, CardRequest.addImageCard request) {
		String imgUrl = s3Service.upload(image, "imageCards");

		ImageCard imageCard = ImageCard.builder()
			.imageUrl(imgUrl)
			.height(request.getBaseImageHeight())
			.width(request.getBaseImageWidth())
			.build();

		ImageCard savedImageCard = imageCardRepository.save(imageCard);

		if (request.getOverlays() != null) {
			for (CardRequest.addImageCardOverlay overlayRequest : request.getOverlays()) {
				Overlay overlay = Overlay.builder()
					.xPosition(overlayRequest.getPositionOfX())
					.yPosition(overlayRequest.getPositionOfY())
					.width(overlayRequest.getWidth())
					.height(overlayRequest.getHeight())
					.imageCard(savedImageCard)
					.build();

				// Overlay를 데이터베이스에 저장
				overlayRepository.save(overlay);
			}
		}

		return savedImageCard.getImageUrl();
	}

	@Transactional
	public CardResponse.getImageCard viewImageCard(Long imageCardId) {
		ImageCard imageCard = imageCardRepository.findById(imageCardId)
			.orElseThrow(() -> new IllegalArgumentException("Image card not found with id: " + imageCardId));

		// 관련된 오버레이 정보를 가져오기
		List<Overlay> overlays = overlayRepository.findByImageCard(imageCard);

		// CardResponse.addImageCard의 오버레이 리스트를 생성
		List<CardRequest.addImageCardOverlay> overlayResponses = overlays.stream().map(overlay ->
				CardRequest.addImageCardOverlay.builder()
					.positionOfX(overlay.getXPosition())
					.positionOfY(overlay.getYPosition())
					.width(overlay.getWidth())
					.height(overlay.getHeight())
					.build())
			.collect(Collectors.toList());

		return CardResponse.getImageCard.builder()
			.imgUrl(imageCard.getImageUrl())
			.baseImageWidth(imageCard.getWidth())
			.baseImageHeight(imageCard.getHeight())
			.overlays(overlayResponses)
			.build();
	}

	@Transactional
	public String editImageCard(CardRequest.addImageCard request, Long imgCardId) {
		// 기존 이미지카드를 ID로 조회
		ImageCard existingImageCard = imageCardRepository.findById(imgCardId)
			.orElseThrow(() -> new IllegalArgumentException("ImageCard not found with ID: " + imgCardId));

		// 기존 이미지카드의 필드를 새로운 값으로 업데이트
		existingImageCard.setHeight(request.getBaseImageHeight());
		existingImageCard.setWidth(request.getBaseImageWidth());

		// 업데이트된 이미지카드를 저장
		ImageCard savedImageCard = imageCardRepository.save(existingImageCard);

		// 기존 오버레이를 삭제하고, 새로운 오버레이를 추가
		overlayRepository.deleteByImageCardId(imgCardId);
		if (request.getOverlays() != null) {
			for (CardRequest.addImageCardOverlay overlayRequest : request.getOverlays()) {
				Overlay overlay = Overlay.builder()
					.xPosition(overlayRequest.getPositionOfX())
					.yPosition(overlayRequest.getPositionOfY())
					.width(overlayRequest.getWidth())
					.height(overlayRequest.getHeight())
					.imageCard(savedImageCard)
					.build();

				// 새로운 오버레이를 데이터베이스에 저장
				overlayRepository.save(overlay);
			}
		}

		return savedImageCard.getImageUrl();
	}


	public void addCard(Card card, Note note){
		Card card_new = Card.builder()
				.note(note)
				.contentsFront(card.getContentsFront())
				.contentsBack(card.getContentsBack())
				.countLearn(0L)
				.build();

		cardRepository.save(card_new);
	}
}
