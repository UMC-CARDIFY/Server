package com.umc.cardify.service;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.umc.cardify.config.exception.BadRequestException;
import com.umc.cardify.config.exception.DatabaseException;
import com.umc.cardify.config.exception.ErrorResponseStatus;
import com.umc.cardify.converter.NoteConverter;
import com.umc.cardify.domain.Card;
import com.umc.cardify.domain.Category;
import com.umc.cardify.domain.Folder;
import com.umc.cardify.domain.Library;
import com.umc.cardify.domain.LibraryCategory;
import com.umc.cardify.domain.Note;
import com.umc.cardify.domain.ProseMirror.Node;
import com.umc.cardify.domain.User;
import com.umc.cardify.domain.enums.CardType;
import com.umc.cardify.domain.enums.MarkStatus;
import com.umc.cardify.dto.note.NoteRequest;
import com.umc.cardify.dto.note.NoteResponse;
import com.umc.cardify.repository.CardRepository;
import com.umc.cardify.repository.CategoryRepository;
import com.umc.cardify.repository.LibraryCategoryRepository;
import com.umc.cardify.repository.LibraryRepository;
import com.umc.cardify.repository.NoteRepository;
import com.umc.cardify.repository.UserRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class NoteService {
	private final NoteRepository noteRepository;
	private final UserRepository userRepository;
	private final CardRepository cardRepository;
	private final LibraryRepository libraryRepository;
	private final CategoryRepository categoryRepository;
	private final LibraryCategoryRepository libraryCategoryRepository;

	private final NoteConverter noteConverter;

	private final ObjectMapper objectMapper;

	public Note getNoteToID(long noteId) {
		return noteRepository.findById(noteId)
			.orElseThrow(() -> new BadRequestException(ErrorResponseStatus.NOT_FOUND_ERROR));
	}

	public Note addNote(Folder folder, Long userId) {
		if (!userId.equals(folder.getUser().getUserId()))
			throw new BadRequestException(ErrorResponseStatus.INVALID_USERID);
		else {
			Note newNote = NoteConverter.toAddNote(folder);
			return noteRepository.save(newNote);
		}
	}

	public NoteResponse.NoteListDTO getNotesByUserId(Long userId, Integer page, Integer size) {
		User user = userRepository.findById(userId)
			.orElseThrow(() -> new BadRequestException(ErrorResponseStatus.INVALID_USERID));

		int getNotePage = (page != null) ? page : 0;
		int getNoteSize = (size != null) ? size : Integer.MAX_VALUE;

		Pageable pageable = PageRequest.of(getNotePage, getNoteSize);
		Page<Note> notePage = noteRepository.findByUser(user, pageable);

		if (notePage == null) {
			throw new DatabaseException(ErrorResponseStatus.NOT_EXIST_NOTE);
		}

		List<NoteResponse.NoteInfoDTO> notes = notePage.getContent()
			.stream()
			.map(noteConverter::toNoteInfoDTO)
			.collect(Collectors.toList());

		return NoteResponse.NoteListDTO.builder()
			.noteList(notes)
			.listsize(notePage.getSize())
			.currentPage(notePage.getNumber() + 1)
			.totalPage(notePage.getTotalPages())
			.totalElements(notePage.getTotalElements())
			.isFirst(notePage.isFirst())
			.isLast(notePage.isLast())
			.build();
	}

	public Boolean deleteNote(Long noteId, Long userId) {
		Note note_del = noteRepository.findById(noteId)
			.orElseThrow(() -> new BadRequestException(ErrorResponseStatus.NOT_FOUND_ERROR));
		if (!userId.equals(note_del.getFolder().getUser().getUserId()))
			throw new BadRequestException(ErrorResponseStatus.INVALID_USERID);
		else {
			noteRepository.delete(note_del);
			return true;
		}
	}

	public NoteResponse.GetNoteToFolderResultDTO getNoteToFolder(Folder folder,
		NoteRequest.GetNoteToFolderDto request) {
		Pageable pageable;

		Integer page = request.getPage();
		if (page == null)
			page = 0;

		Integer size = request.getSize();
		if (size == null)
			size = folder.getNotes().size();

		String order = request.getOrder();
		if (order == null)
			order = "asc";

		pageable = switch (order.toLowerCase()) {
			case "asc" -> PageRequest.of(page, size, Sort.by(Sort.Order.asc("markAt"), Sort.Order.asc("name")));
			case "desc" -> PageRequest.of(page, size, Sort.by(Sort.Order.asc("markAt"), Sort.Order.desc("name")));
			case "edit-newest" ->
				PageRequest.of(page, size, Sort.by(Sort.Order.asc("markAt"), Sort.Order.asc("editDate")));
			case "edit-oldest" ->
				PageRequest.of(page, size, Sort.by(Sort.Order.asc("markAt"), Sort.Order.desc("editDate")));
			default -> throw new BadRequestException(ErrorResponseStatus.REQUEST_ERROR);
		};
		Page<Note> notes_all = noteRepository.findByFolder(folder, pageable);

		NoteResponse.GetNoteToFolderResultDTO noteDTO = noteConverter.toGetNoteToFolderResult(folder, notes_all);
		return noteDTO;
	}

	public Boolean markNote(Long noteId, Boolean isMark, Long userId) {
		Note note_mark = noteRepository.findById(noteId)
			.orElseThrow(() -> new BadRequestException(ErrorResponseStatus.NOT_FOUND_ERROR));
		if (!userId.equals(note_mark.getFolder().getUser().getUserId()))
			throw new BadRequestException(ErrorResponseStatus.INVALID_USERID);
		else {
			if (isMark) {
				note_mark.setMarkState(MarkStatus.ACTIVE);
				note_mark.setMarkAt(LocalDateTime.now());
			} else if (!isMark) {
				note_mark.setMarkState(MarkStatus.INACTIVE);
				note_mark.setMarkAt(null);
			} else
				throw new BadRequestException(ErrorResponseStatus.REQUEST_ERROR);
			noteRepository.save(note_mark);
			return true;
		}
	}

	@Transactional
	public Boolean writeNote(NoteRequest.WriteNoteDto request, Long userId) {
		Note note = noteRepository.findById(request.getNoteId())
			.orElseThrow(() -> new BadRequestException(ErrorResponseStatus.NOT_FOUND_ERROR));

		if (!userId.equals(note.getFolder().getUser().getUserId())) {
			log.warn("Invalid userId: {}", userId);
			throw new BadRequestException(ErrorResponseStatus.INVALID_USERID);
		} else if (libraryRepository.findByNote(note) != null) {
			log.warn("Attempt to insert a note that already exists in the library: {}", note.getNoteId());
			throw new BadRequestException(ErrorResponseStatus.DB_INSERT_ERROR);
		}

		StringBuilder totalText = new StringBuilder();
		note.setName(request.getName());

		Node node = request.getContents();
		searchCard(node, totalText, note); // CardService의 searchCard 호출
		note.setTotalText(totalText.toString());

		try {
			String jsonStr = objectMapper.writeValueAsString(node);
			note.setContents(jsonStr);
		} catch (JsonProcessingException e) {
			log.error("Failed to serialize note contents to JSON", e);
			throw new BadRequestException(ErrorResponseStatus.JSON_PROCESSING_ERROR);
		}

		noteRepository.save(note);
		return true;
	}

	public void searchCard(Node node, StringBuilder input, Note note) {
		if (isCardNode(node)) {
			processCardNode(node, input, note);
		} else if (node.getType().equals("text")) {
			processTextNode(node, input);
		}

		if (node.getContent() != null) {
			node.getContent().forEach(content -> searchCard(content, input, note));
		}
	}

	private boolean isCardNode(Node node) {
		return node.getType().equals("word_card") || node.getType().equals("blank_card") || node.getType()
			.equals("multi_card");
	}

	private void processCardNode(Node node, StringBuilder input, Note note) {
		String answer = String.join(" ", node.getAttrs().getAnswer());
		String questionFront = node.getAttrs().getQuestion_front();
		String questionBack = node.getAttrs().getQuestion_back();

		if (questionFront == null)
			questionFront = "";
		if (questionBack == null)
			questionBack = "";

		String nodeText = questionFront + answer + questionBack;
		if (!nodeText.endsWith("."))
			nodeText += ".";
		input.append(nodeText);

		switch (node.getType()) {
			case "blank_card" -> {
				Card card = createCard(note, questionFront, questionBack, answer, CardType.BLANK);
				cardRepository.save(card);
			}
			case "multi_card" -> {
				Card card = createCard(note, questionFront, null, answer, CardType.MULTI);
				cardRepository.save(card);
			}
			case "word_card" -> {
				Card card = createCard(note, questionFront, null, answer, CardType.WORD);
				cardRepository.save(card);
			}
		}
	}

	private Card createCard(Note note, String questionFront, String questionBack, String answer, CardType cardType) {
		return Card.builder()
			.note(note)
			.contentsFront(questionFront)
			.contentsBack(questionBack)
			.answer(answer)
			.isLearn(false)
			.countLearn(0L)
			.type(cardType.getValue())  // 카드 타입에 따라 저장
			.build();
	}

	public void processTextNode(Node node, StringBuilder input) {
		String nodeText = node.getText();
		if (!nodeText.endsWith("."))
			nodeText += ".";
		input.append(nodeText);
	}

	public List<NoteResponse.SearchNoteResDTO> searchNote(Folder folder, String search) {
		//문단 구분점인 .을 입력시 빈 리스트 반환
		if (search.trim().equals("."))
			return null;

		List<Note> notes = noteRepository.findByFolder(folder)
			.stream()
			.filter(note -> note.getName().contains(search) | note.getTotalText().contains(search))
			.toList();
		List<NoteResponse.SearchNoteResDTO> searchList = notes.stream()
			.map(list -> noteConverter.toSearchNoteResult(list, search))
			.collect(Collectors.toList());

		return searchList;
	}

	public Boolean shareLib(Long userId, NoteRequest.ShareLibDto request) {
		Note note = getNoteToID(request.getNoteId());
		if (!userId.equals(note.getFolder().getUser().getUserId()))
			throw new BadRequestException(ErrorResponseStatus.INVALID_USERID);
		if (note.getDownloadLibId() != null)
			throw new BadRequestException(ErrorResponseStatus.DB_INSERT_ERROR);
		//기존에 공유되어 있던 데이터를 삭제
		Library library = note.getLibrary();
		if (library != null) {
			note.setLibrary(null);
			libraryRepository.delete(library);
		}

		Library library_new = Library.builder().note(note).uploadAt(LocalDateTime.now()).build();
		libraryRepository.save(library_new);

		//카테고리 찾아서 Library에 삽입
		List<Category> categoryList = null;
		if (request.getCategory().size() > 0 && request.getCategory().size() <= 3) {
			categoryList = request.getCategory().stream().map(categoryStr -> {
				Category category = categoryRepository.findByName(categoryStr);
				//요청한 카테고리가 없으면 에러
				if (category == null)
					throw new BadRequestException(ErrorResponseStatus.NOT_FOUND_CATEGORY);
				return category;
			}).toList();
		} else
			throw new BadRequestException(ErrorResponseStatus.REQUEST_ERROR);
		if (categoryList != null) {
			categoryList.stream()
				.map(category -> libraryCategoryRepository.save(
					LibraryCategory.builder().category(category).library(library_new).build()))
				.toList();
		}

		noteRepository.save(note);
		return true;
	}

	public Boolean cancelShare(Long noteId, Long userId) {
		Note note = getNoteToID(noteId);
		if (!userId.equals(note.getFolder().getUser().getUserId()))
			throw new BadRequestException(ErrorResponseStatus.INVALID_USERID);
		Library library = note.getLibrary();

		note.setLibrary(null);
		noteRepository.save(note);

		if (library != null)
			libraryRepository.delete(library);
		return true;
	}

	public NoteResponse.getNoteDTO getNote(Long noteId) {
		Note note = noteRepository.findById(noteId)
			.orElseThrow(() -> new BadRequestException(ErrorResponseStatus.NOT_FOUND_ERROR));
		List<NoteResponse.getNoteCardDTO> cardDTO = note.getCards().stream().map(card -> {
			return NoteResponse.getNoteCardDTO.builder()
				.cardId(card.getCardId())
				.cardName(note.getName())
				.contentsFront(card.getContentsFront())
				.contentsBack(card.getContentsBack())
				.build();
		}).toList();

		return NoteResponse.getNoteDTO.builder()
			.noteId(note.getNoteId())
			.noteName(note.getName())
			.noteContent(note.getContents())
			.cardList(cardDTO)
			.build();
	}

	@Transactional(readOnly = true)
	public NoteResponse.NoteListDTO filterColorsNotes(Long userId, Integer page, Integer size, String colors) {
		User user = userRepository.findById(userId)
			.orElseThrow(() -> new BadRequestException(ErrorResponseStatus.INVALID_USERID));

		int filterNotePage = (page != null) ? page : 0;
		int filterNoteSize = (size != null) ? size : 30;

		if (colors == null || colors.isEmpty()) {
			throw new DatabaseException(ErrorResponseStatus.NOT_EXIST_NOTE);
		}

		List<String> colorList = Arrays.asList(colors.split(","));

		Pageable pageable = PageRequest.of(filterNotePage, filterNoteSize);
		Page<Note> notePage = noteRepository.findByNoteIdAndUser(user, colorList, pageable);

		if (notePage.isEmpty()) {
			throw new DatabaseException(ErrorResponseStatus.NOT_EXIST_NOTE);
		}

		List<NoteResponse.NoteInfoDTO> notes = notePage.getContent()
			.stream()
			.map(noteConverter::toNoteInfoDTO)
			.collect(Collectors.toList());

		return NoteResponse.NoteListDTO.builder()
			.noteList(notes)
			.listsize(filterNoteSize)
			.currentPage(filterNotePage + 1)
			.totalPage(notePage.getTotalPages())
			.totalElements(notePage.getTotalElements())
			.isFirst(notePage.isFirst())
			.isLast(notePage.isLast())
			.build();
	}

}