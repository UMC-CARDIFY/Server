package com.umc.cardify.service;

import static com.umc.cardify.config.exception.ErrorResponseStatus.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.umc.cardify.config.exception.BadRequestException;
import com.umc.cardify.domain.Card;
import com.umc.cardify.domain.ImageCard;
import com.umc.cardify.domain.Note;
import com.umc.cardify.domain.Overlay;
import com.umc.cardify.domain.StudyCardSet;
import com.umc.cardify.domain.StudyLog;
import com.umc.cardify.dto.card.CardRequest;
import com.umc.cardify.dto.card.CardResponse;
import com.umc.cardify.repository.ImageCardRepository;
import com.umc.cardify.repository.OverlayRepository;
import com.umc.cardify.repository.StudyCardSetRepository;
import com.umc.cardify.repository.StudyLogRepository;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class CardComponentService {
	private final CardModuleService cardModuleService;
	private final NoteModuleService noteModuleService;
	private final S3Service s3Service;

	private final ImageCardRepository imageCardRepository;
	private final OverlayRepository overlayRepository;
	private final StudyLogRepository studyLogRepository;
	private final StudyCardSetRepository studyCardSetRepository;

	@Transactional
	public void completeStudy(Long studyCardSetId) {
		StudyCardSet studyCardSet = cardModuleService.getStudyCardSetById(studyCardSetId);

		List<Card> cards = cardModuleService.getCardsByStudyCardSet(studyCardSet);

		int remainingCardsCount = cards.size();
		StudyLog studyLog = StudyLog.builder()
			.studyDate(LocalDateTime.now())
			.studyCardNumber(remainingCardsCount)
			.studyCardSet(studyCardSet)
			.build();

		studyLogRepository.save(studyLog);

		// 3. 난이도(difficulty)가 PASS 인 카드를 삭제
		List<Card> cardsToRemove = cards.stream()
			.filter(card -> card.getDifficulty().getValue() == 4)
			.collect(Collectors.toList());
		cardModuleService.deleteAll(cardsToRemove);

		studyCardSet.setRecentStudyDate(LocalDateTime.now());

		studyCardSetRepository.save(studyCardSet);
	}


	@Transactional
	public String addImageCard(MultipartFile image, CardRequest.addImageCard request) {
		String imgUrl = s3Service.upload(image, "imageCards");

		ImageCard imageCard = ImageCard.builder()
			.imageUrl(imgUrl)
			.height(request.getBaseImageHeight())
			.width(request.getBaseImageWidth())
			.build();

		Note note = noteModuleService.getNoteById(request.getNoteId());

		StudyCardSet studyCardSet = cardModuleService.findStudyCardSetByNote(note);

		imageCard.setStudyCardSet(studyCardSet);
		cardModuleService.saveStudyCardSet(studyCardSet);

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

				overlayRepository.save(overlay);
			}
		}

		return savedImageCard.getImageUrl();
	}

	@Transactional
	public CardResponse.getImageCard viewImageCard(Long imageCardId) {
		ImageCard imageCard = imageCardRepository.findById(imageCardId)
			.orElseThrow(() -> new IllegalArgumentException("Image card not found with id: " + imageCardId));

		List<Overlay> overlays = overlayRepository.findByImageCard(imageCard);

		List<CardRequest.addImageCardOverlay> overlayResponses = overlays.stream()
			.map(overlay -> CardRequest.addImageCardOverlay.builder()
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
		ImageCard existingImageCard = imageCardRepository.findById(imgCardId)
			.orElseThrow(() -> new IllegalArgumentException("ImageCard not found with ID: " + imgCardId));

		existingImageCard.setHeight(request.getBaseImageHeight());
		existingImageCard.setWidth(request.getBaseImageWidth());

		ImageCard savedImageCard = imageCardRepository.save(existingImageCard);

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

				overlayRepository.save(overlay);
			}
		}

		return savedImageCard.getImageUrl();
	}

	@Transactional
	public Page<CardResponse.getStudyCardSetLists> getStudyCardSetLists(Long userId, Pageable pageable) {
		Page<StudyCardSet> studyCardSets = cardModuleService.getStudyCardSetsByUser(userId, pageable);

		List<CardResponse.getStudyCardSetLists> cardLists = studyCardSets.stream()
			.map(studyCardSet -> CardResponse.getStudyCardSetLists.builder()
				.studyStatus(studyCardSet.getStudyStatus().getDescription())
				.noteName(studyCardSet.getNoteName())
				.color(studyCardSet.getColor())
				.folderName(studyCardSet.getFolder().getName())
				.recentStudyDate(studyCardSet.getRecentStudyDate())
				.nextStudyDate(studyCardSet.getNextStudyDate())
				.studyCardSetId(studyCardSet.getId())
				.build())
			.collect(Collectors.toList());

		return new PageImpl<>(cardLists, pageable, studyCardSets.getTotalElements());
	}

	@Transactional
	public Page<CardResponse.getCardLists> getCardLists(Long studyCardSetId, int pageNumber) {
		StudyCardSet studyCardSet = cardModuleService.getStudyCardSetById(studyCardSetId);

		List<Card> cards = cardModuleService.getCardsByStudyCardSet(studyCardSet);

		int totalCards = cards.size();

		// 페이지당 1개의 카드만 보여주기 위해 PageRequest에서 size를 1로 설정
		Pageable pageable = PageRequest.of(pageNumber, 1);

		int start = (int)pageable.getOffset();
		int end = Math.min((start + pageable.getPageSize()), totalCards);
		List<Card> pagedCards = cards.subList(start, end);

		Page<Card> cardsPage = new PageImpl<>(pagedCards, pageable, totalCards);

		return cardsPage.map(card -> {
			return CardResponse.getCardLists.builder()
				.contentsFront(card.getContentsFront())
				.contentsBack(card.getContentsBack())
				.answer(card.getAnswer())
				.build();
		});
	}

	public void updateCardDifficulty(Long cardId, int difficulty) {
		if (difficulty > 4 || difficulty < 1) {
			throw new BadRequestException(NOT_EXIST_DIFFICULTY_CODE);
		}

		Card card = cardModuleService.getCardById(cardId);
		card.setDifficulty(difficulty);

		cardModuleService.updateCardDifficulty(card);
	}

	public CardResponse.cardStudyGraph viewStudyCardGraph(Long studyCardSetId) {
		StudyCardSet studyCardSet = cardModuleService.getStudyCardSetById(studyCardSetId);

		List<Card> cards = cardModuleService.getCardsByStudyCardSet(studyCardSet);

		int easyCardsCount = 0;
		int normalCardsCount = 0;
		int hardCardsCount = 0;
		int passCardsCount = 0;

		int totalCards = cards.size();

		for (Card card : cards) {
			switch (card.getDifficulty()) {
				case NONE:
					continue;
				case EASY:
					easyCardsCount++;
					break;
				case NORMAL:
					normalCardsCount++;
					break;
				case HARD:
					hardCardsCount++;
					break;
				case PASS:
					passCardsCount++;
					break;
			}
		}

		int easyCardsPercent = (easyCardsCount * 100) / totalCards;
		int normalCardsPercent = (normalCardsCount * 100) / totalCards;
		int hardCardsPercent = (hardCardsCount * 100) / totalCards;
		int passCardsPercent = (passCardsCount * 100) / totalCards;

		return CardResponse.cardStudyGraph.builder()
			.easyCardsNumber(easyCardsCount)
			.normalCardsNumber(normalCardsCount)
			.hardCardsNumber(hardCardsCount)
			.passCardsNumber(passCardsCount)
			.easyCardsPercent(easyCardsPercent)
			.normalCardsPercent(normalCardsPercent)
			.hardCardsPercent(hardCardsPercent)
			.passCardsPercent(passCardsPercent)
			.build();
	}

	public void addCardToNote(Card card, Note note) {
		Card newCard = Card.builder()
			.note(note)
			.contentsFront(card.getContentsFront())
			.contentsBack(card.getContentsBack())
			.countLearn(0L)
			.build();

		cardModuleService.saveCard(newCard);
	}
}

