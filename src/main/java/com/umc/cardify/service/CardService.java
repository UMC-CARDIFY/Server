package com.umc.cardify.service;

import com.umc.cardify.domain.Card;
import com.umc.cardify.domain.Note;
import com.umc.cardify.repository.CardRepository;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.umc.cardify.domain.ImageCard;
import com.umc.cardify.domain.Overlay;
import com.umc.cardify.dto.card.CardRequest;
import com.umc.cardify.repository.ImageCardRepository;
import com.umc.cardify.repository.OverlayRepository;

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
	public String addImageCard(Long userId, MultipartFile image, CardRequest.addImageCard request) {
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
	public void addCard(CardRequest.WriteCardDto cardDto, Note note){
		String contents_front = cardDto.getText();
		String contents_back = contents_front
				.replace(">>", "")
				.replace("<<", "")
				.replace("{{", "")
				.replace("}}", "")
				.replace("==", "");

		Card card = Card.builder()
				.note(note)
				.name(cardDto.getName())
				.contentsFront(contents_front)
				.contentsBack(contents_back)
				.countLearn(0L)
				.build();

		cardRepository.save(card);

	}
}
