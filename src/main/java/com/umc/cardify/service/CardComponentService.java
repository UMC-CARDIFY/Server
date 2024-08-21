package com.umc.cardify.service;

import static com.umc.cardify.config.exception.ErrorResponseStatus.*;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.umc.cardify.config.exception.BadRequestException;
import com.umc.cardify.config.exception.DatabaseException;
import com.umc.cardify.domain.Card;
import com.umc.cardify.domain.ImageCard;
import com.umc.cardify.domain.Note;
import com.umc.cardify.domain.Overlay;
import com.umc.cardify.domain.StudyCardSet;
import com.umc.cardify.domain.StudyLog;
import com.umc.cardify.domain.enums.Difficulty;
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
	private final StudyCardSetRepository studyCardSetRepository;
	private final StudyLogRepository studyLogRepository;
	@Transactional
	public void deleteStudyCardSet(Long studyCardSetId) {
		StudyCardSet studyCardSet = cardModuleService.getStudyCardSetById(studyCardSetId);
		cardModuleService.deleteCardSet(studyCardSetId);
		noteModuleService.deleteNoteById(studyCardSet.getNote().getNoteId());

	}

	@Transactional
	public List<CardResponse.getStudySuggestion> suggestionAnalyzeStudy(Long userId, Timestamp date) {
		List<Card> cards = cardModuleService.findAllByUserIdAndLearnNextTimeAfter(userId, date);

		// 조회된 카드를 CardResponse.getStudySuggestion DTO로 변환
		return cards.stream()
			.map(card -> CardResponse.getStudySuggestion.builder()
				.remainTime(card.getLearnNextTime())
				.noteName(card.getStudyCardSet().getNote().getName())
				.folderName(card.getStudyCardSet().getFolder().getName())
				.build())
			.collect(Collectors.toList());
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
				.markStatus(studyCardSet.getNote().getMarkState())
				.build())
			.collect(Collectors.toList());

		return new PageImpl<>(cardLists, pageable, studyCardSets.getTotalElements());
	}

	@Transactional
	public Page<Object> getCardLists(Long studyCardSetId, int pageNumber) {
		StudyCardSet studyCardSet = cardModuleService.getStudyCardSetById(studyCardSetId);

		List<Card> cards = cardModuleService.getCardsByStudyCardSet(studyCardSet);
		List<ImageCard> imageCards = cardModuleService.getImageCardsByStudyCardSet(studyCardSet);

		List<Object> allCards = new ArrayList<>();
		allCards.addAll(cards);
		allCards.addAll(imageCards);
		log.debug("All cards combined: {}", allCards);

		allCards.sort((a, b) -> {
			LocalDateTime createdA = (a instanceof Card) ? ((Card)a).getCreatedAt() : ((ImageCard)a).getCreatedAt();
			LocalDateTime createdB = (b instanceof Card) ? ((Card)b).getCreatedAt() : ((ImageCard)b).getCreatedAt();
			return createdA.compareTo(createdB);
		});

		int totalCards = allCards.size();
		Pageable pageable = PageRequest.of(pageNumber, 1);

		int start = (int)pageable.getOffset();
		int end = Math.min((start + pageable.getPageSize()), totalCards);

		List<Object> pagedCards = allCards.subList(start, end);

		Page<Object> cardsPage = new PageImpl<>(pagedCards, pageable, totalCards);

		return cardsPage.map(card -> {
			if (card instanceof Card wordCard) {
				log.debug("Mapping WordCard: {}", wordCard.getCardId());
				return mapToWordCardResponse(wordCard, studyCardSet);
			} else if (card instanceof ImageCard imageCard) {
				log.debug("Mapping ImageCard: {}", imageCard.getId());
				return mapToImageCardResponse(imageCard, studyCardSet);
			} else {
				log.error("Unexpected card type encountered: {}", card.getClass().getName());
				throw new IllegalStateException("Unexpected card type");
			}
		});
	}

	private CardResponse.getCardLists mapToWordCardResponse(Card card, StudyCardSet studyCardSet) {
		return CardResponse.getCardLists.builder()
			.contentsFront(card.getContentsFront())
			.contentsBack(card.getContentsBack())
			.answer(card.getAnswer())
			.cardId(card.getCardId())
			.noteId(studyCardSet.getNote().getNoteId())
			.folderId(studyCardSet.getFolder().getFolderId())
			.cardType("word")
			.build();
	}

	private CardResponse.getImageCard mapToImageCardResponse(ImageCard imageCard, StudyCardSet studyCardSet) {
		return CardResponse.getImageCard.builder()
			.imgUrl(imageCard.getImageUrl())
			.baseImageWidth(imageCard.getWidth())
			.baseImageHeight(imageCard.getHeight())
			.overlays(convertOverlays(imageCard.getOverlays()))
			.imageCardId(imageCard.getId())
			.noteId(studyCardSet.getNote().getNoteId())
			.folderId(studyCardSet.getFolder().getFolderId())
			.cardType("image")
			.build();
	}

	private List<CardRequest.addImageCardOverlay> convertOverlays(List<Overlay> overlays) {
		List<CardRequest.addImageCardOverlay> overlayDtos = new ArrayList<>();
		for (Overlay overlay : overlays) {
			overlayDtos.add(CardRequest.addImageCardOverlay.builder()
				.positionOfX(overlay.getXPosition())
				.positionOfY(overlay.getYPosition())
				.width(overlay.getWidth())
				.height(overlay.getHeight())
				.build());
		}
		return overlayDtos;
	}

	public void updateCardDifficulty(CardRequest.difficulty request) {
		if (request.getDifficulty() > 4 || request.getDifficulty() < 1) {
			throw new BadRequestException(NOT_EXIST_DIFFICULTY_CODE);
		}

		if (request.getCardType() == 0) {
			Card card = cardModuleService.getCardById(request.getCardId());
			card.setDifficulty(request.getDifficulty());
			cardModuleService.updateWordCardDifficulty(card);
		} else {
			ImageCard imageCard = cardModuleService.getImageCardById(request.getCardId());
			imageCard.setDifficulty(request.getDifficulty());
			cardModuleService.updateImageCardDifficulty(imageCard);
		}

	}

	public CardResponse.cardStudyGraph viewStudyCardGraph(Long studyCardSetId) {
		StudyCardSet studyCardSet = cardModuleService.getStudyCardSetById(studyCardSetId);

		List<Card> cards = cardModuleService.getCardsByStudyCardSet(studyCardSet);
		List<ImageCard> imageCards = cardModuleService.getImageCardsByStudyCardSet(studyCardSet);

		int easyCardsCount = 0;
		int normalCardsCount = 0;
		int hardCardsCount = 0;
		int passCardsCount = 0;

		int totalCards = cards.size() + imageCards.size();

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

		for (ImageCard card : imageCards) {
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

	@Transactional
	public Page<CardResponse.getStudyLog> viewStudyLog(Long studyCardSetId, int page, int size) {
		Pageable pageable = PageRequest.of(page, size);

		StudyCardSet studyCardSet = studyCardSetRepository.findById(studyCardSetId)
			.orElseThrow(() -> new DatabaseException(NOT_FOUND_ERROR));

		Page<StudyLog> studyLogsPage = studyLogRepository.findByStudyCardSet(studyCardSet, pageable);

		return studyLogsPage.map(studyLog -> CardResponse.getStudyLog.builder()
			.cardNumber(studyLog.getStudyCardNumber())
			.studyDate(studyLog.getStudyDate())
			.build());
	}

	public Timestamp calculateNextStudyTime(Card card) {
		LocalDateTime currentTime = LocalDateTime.now();
		long baseInterval; // 기본 학습 간격 (분 단위로 계산)
		double increaseFactor; // 증가 비율
		long currentInterval; // 현재 학습 간격

		// 난이도가 NONE인 경우, 학습을 하지 않으므로 null 반환
		if (card.getDifficulty() == Difficulty.NONE) {
			log.debug("Card {} has difficulty NONE, skipping calculation.", card.getCardId());
			return null;
		}

		switch (card.getDifficulty()) {
			case HARD: // 난이도 1
				baseInterval = 30; // 30분 (0.5시간)
				increaseFactor = 1;
				break;
			case NORMAL: // 난이도 2
				baseInterval = 60; // 1시간
				increaseFactor = 1.5;
				break;
			case EASY: // 난이도 3
				baseInterval = 48 * 60; // 48시간 (2일)
				increaseFactor = 2;
				break;
			default:
				baseInterval = 60; // 기본적으로 1시간으로 설정
				increaseFactor = 1;
		}

		log.debug("Card {} - Difficulty: {}", card.getCardId(), card.getDifficulty());
		log.debug("Card {} - Base Interval (minutes): {}", card.getCardId(), baseInterval);
		log.debug("Card {} - Increase Factor: {}", card.getCardId(), increaseFactor);

		if (card.getCountLearn() == 0) {
			// 첫 학습인 경우
			currentInterval = baseInterval;
			card.setLearnLastTime(Timestamp.valueOf(currentTime)); // 학습 시점을 기록
			card.setLearnNextTime(Timestamp.valueOf(currentTime.plusMinutes(currentInterval)));
		} else {
			// 이미 학습된 카드인 경우
			long timeSinceLastLearn = card.getLearnLastTime().toLocalDateTime().until(currentTime, ChronoUnit.MINUTES);
			log.debug("Card {} - Time Since Last Learn (minutes): {}", card.getCardId(), timeSinceLastLearn);
			currentInterval = (long)(baseInterval * increaseFactor); // 기본 간격에 증가 비율을 적용
			card.setLearnNextTime(Timestamp.valueOf(currentTime.plusMinutes(currentInterval)));
		}

		card.setCountLearn(card.getCountLearn() + 1); // 학습 횟수를 증가시킴

		log.debug("Card {} - Current Interval (minutes): {}", card.getCardId(), currentInterval);
		log.debug("Card {} - Next Study Time: {}", card.getCardId(), card.getLearnNextTime());

		return card.getLearnNextTime();
	}

	@Transactional
	public void completeStudy(Long studyCardSetId) {
		StudyCardSet studyCardSet = cardModuleService.getStudyCardSetById(studyCardSetId);

		List<Card> cards = cardModuleService.getCardsByStudyCardSet(studyCardSet);
		List<ImageCard> imageCards = cardModuleService.getImageCardsByStudyCardSet(studyCardSet);

		int remainingCardsCount = cards.size() + imageCards.size();

		StudyLog studyLog = StudyLog.builder()
			.studyDate(LocalDateTime.now())
			.studyCardNumber(remainingCardsCount)
			.studyCardSet(studyCardSet)
			.user(studyCardSet.getUser())
			.build();

		studyLogRepository.save(studyLog);

		// 난이도가 4인 카드들 제거
		List<Card> cardsToRemove = cards.stream()
			.filter(card -> card.getDifficulty() == Difficulty.PASS)
			.collect(Collectors.toList());
		cardModuleService.deleteAllCards(cardsToRemove);

		Timestamp earliestNextStudyTime = null;
		for (Card card : cards) {
			Timestamp nextStudyTime = calculateNextStudyTime(card);
			card.setLearnNextTime(nextStudyTime);
			card.setLearnLastTime(Timestamp.valueOf(LocalDateTime.now()));
			cardModuleService.saveCard(card);

			// Update the earliest next study time
			if (earliestNextStudyTime == null || nextStudyTime.before(earliestNextStudyTime)) {
				System.out.println("nextStudyTime = " + nextStudyTime);
				earliestNextStudyTime = nextStudyTime;
			}
		}

		// Update the studyCardSet with the recent study date and next study date
		studyCardSet.setRecentStudyDate(LocalDateTime.now());
		if (earliestNextStudyTime != null) {
			System.out.println("earliestNextStudyTime = " + earliestNextStudyTime);
			studyCardSet.setNextStudyDate(earliestNextStudyTime.toLocalDateTime());
		}
		studyCardSetRepository.save(studyCardSet);
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

