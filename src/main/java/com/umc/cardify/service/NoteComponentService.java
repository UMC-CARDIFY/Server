package com.umc.cardify.service;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.umc.cardify.config.exception.BadRequestException;
import com.umc.cardify.config.exception.DatabaseException;
import com.umc.cardify.config.exception.ErrorResponseStatus;
import com.umc.cardify.converter.NoteConverter;
import com.umc.cardify.domain.Category;
import com.umc.cardify.domain.ContentsNote;
import com.umc.cardify.domain.Folder;
import com.umc.cardify.domain.Library;
import com.umc.cardify.domain.LibraryCategory;
import com.umc.cardify.domain.Note;
import com.umc.cardify.domain.ProseMirror.Node;
import com.umc.cardify.domain.User;
import com.umc.cardify.domain.enums.MarkStatus;
import com.umc.cardify.dto.note.NoteRequest;
import com.umc.cardify.dto.note.NoteResponse;
import com.umc.cardify.repository.CategoryRepository;
import com.umc.cardify.repository.ContentsNoteRepository;
import com.umc.cardify.repository.LibraryCategoryRepository;
import com.umc.cardify.repository.LibraryRepository;
import com.umc.cardify.repository.NoteRepository;
import com.umc.cardify.repository.UserRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class NoteComponentService {
	private final NoteRepository noteRepository;
	private final UserRepository userRepository;
	private final LibraryRepository libraryRepository;
	private final CategoryRepository categoryRepository;
	private final LibraryCategoryRepository libraryCategoryRepository;
	private final ContentsNoteRepository contentsNoteRepository;

	private final NoteModuleService noteModuleService;
	private final CardModuleService cardModuleService;

	private final NoteConverter noteConverter;

	private final ObjectMapper objectMapper;

	public Note addNote(Folder folder, Long userId) {
		if (!userId.equals(folder.getUser().getUserId()))
			throw new BadRequestException(ErrorResponseStatus.INVALID_USERID);
		else {
			Note newNote = NoteConverter.toAddNote(folder);
			noteRepository.save(newNote);
			ContentsNote contentsNote = ContentsNote.builder().noteId(newNote.getNoteId()).build();
			contentsNoteRepository.save(contentsNote);
			newNote.setContentsId(contentsNote.getContentsId());
			noteRepository.save(newNote);
			return newNote;
		}
	}

	public Boolean deleteNote(Long noteId, Long userId) {
		Note note_del = noteRepository.findById(noteId)
			.orElseThrow(() -> new BadRequestException(ErrorResponseStatus.NOT_FOUND_ERROR));
		if (!userId.equals(note_del.getFolder().getUser().getUserId()))
			throw new BadRequestException(ErrorResponseStatus.INVALID_USERID);
		else {
			noteRepository.delete(note_del);
			contentsNoteRepository.delete(contentsNoteRepository.findByNoteId(note_del.getNoteId()));
			return true;
		}
	}

	@Transactional
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
			order = "create-newest";

		pageable = switch (order.toLowerCase()) {
			case "asc" -> PageRequest.of(page, size, Sort.by(Sort.Order.asc("markAt"), Sort.Order.asc("name")));
			case "desc" -> PageRequest.of(page, size, Sort.by(Sort.Order.asc("markAt"), Sort.Order.desc("name")));
			case "edit-newest" ->
				PageRequest.of(page, size, Sort.by(Sort.Order.asc("markAt"), Sort.Order.desc("editDate")));
			case "edit-oldest" ->
				PageRequest.of(page, size, Sort.by(Sort.Order.asc("markAt"), Sort.Order.asc("editDate")));
			case "create-newest" ->
				PageRequest.of(page, size, Sort.by(Sort.Order.asc("markAt"), Sort.Order.desc("createdAt")));
			case "create-oldest" ->
				PageRequest.of(page, size, Sort.by(Sort.Order.asc("markAt"), Sort.Order.asc("createdAt")));
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
	public Boolean writeNote(NoteRequest.WriteNoteDto request, Long userId, List<MultipartFile> images) {
		Note note = noteModuleService.getNoteById(request.getNoteId());
		System.out.println("note.getNoteId() = " + note.getNoteId());

		if (!userId.equals(note.getFolder().getUser().getUserId())) {
			log.warn("Invalid userId: {}", userId);
			throw new BadRequestException(ErrorResponseStatus.INVALID_USERID);
		}
		if (!note.getIsEdit()) {
			log.warn("IsEdit is : {}", note.getIsEdit());
			throw new BadRequestException(ErrorResponseStatus.DB_UPDATE_ERROR);
		}
		if (cardModuleService.existsByNote(note)) {
			cardModuleService.deleteAllCardsByNoteId(note.getNoteId());
			cardModuleService.deleteAllImageCardsByNoteId(note.getNoteId());

		}

		StringBuilder totalText = new StringBuilder();
		note.setName(request.getName());

		Queue<MultipartFile> imageQueue = new LinkedList<>(images != null ? images : Collections.emptyList());
		Node node = request.getContents();
		searchCard(node, totalText, note, imageQueue);
		note.setTotalText(totalText.toString());

		ContentsNote contentsNote = contentsNoteRepository.findByNoteId(note.getNoteId());
		contentsNote.setContents(node);
		contentsNoteRepository.save(contentsNote);

		noteModuleService.saveNote(note);
		return true;
	}

	public void searchCard(Node node, StringBuilder input, Note note, Queue<MultipartFile> imageQueue) {
		if (node.getType().endsWith("card")) {
			cardModuleService.processCardNode(node, input, note, imageQueue);
		} else if (node.getType().equals("text")) {
			processTextNode(node, input);
		}

		if (node.getContent() != null) {
			node.getContent().forEach(content -> searchCard(content, input, note, imageQueue));
		}
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
		Note note = noteModuleService.getNoteById(request.getNoteId());
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

		List<Category> categoryList = null;
		if (request.getCategory().size() > 0 && request.getCategory().size() <= 3) {
			categoryList = request.getCategory().stream().map(categoryStr -> {
				Category category = categoryRepository.findByName(categoryStr);
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

		note.setIsEdit(false);
		noteRepository.save(note);
		return true;
	}

	public Boolean cancelShare(Long noteId, Long userId) {
		Note note = noteModuleService.getNoteById(noteId);
		if (!userId.equals(note.getFolder().getUser().getUserId()))
			throw new BadRequestException(ErrorResponseStatus.INVALID_USERID);
		Library library = note.getLibrary();

		note.setLibrary(null);
		note.setIsEdit(true);
		noteRepository.save(note);

		if (library != null)
			libraryRepository.delete(library);
		return true;
	}

	public NoteResponse.getNoteDTO getNote(Long noteId) {
		Note note = noteRepository.findById(noteId)
			.orElseThrow(() -> new BadRequestException(ErrorResponseStatus.NOT_FOUND_ERROR));
		//노트 조회 시간 갱신
		note.setViewAt(LocalDateTime.now());
		noteRepository.save(note);

		//노트 내용 반환
		List<NoteResponse.getNoteCardDTO> cardDTO = note.getCards().stream().map(card -> {
			return NoteResponse.getNoteCardDTO.builder()
				.cardId(card.getCardId())
				.cardName(note.getName())
				.contentsFront(card.getContentsFront())
				.contentsBack(card.getContentsBack())
				.build();
		}).toList();

		return noteConverter.getNoteDTO(note, cardDTO);
	}

	public List<NoteResponse.NoteInfoDTO> getRecentNotes(Long userId, int page, Integer size) {
		User user = userRepository.findById(userId)
			.orElseThrow(() -> new BadRequestException(ErrorResponseStatus.INVALID_USERID));
		int recentNoteSize = (size != null) ? size : 5;
		Pageable pageable = PageRequest.of(page, recentNoteSize);
		Page<Note> notes = noteRepository.findByUserOrderByViewAtDesc(user, pageable);
		return notes.stream().map(noteConverter::recentNoteInfoDTO).collect(Collectors.toList());
	}

	// 노트 조회, 정렬, 필터링 통합 Service
	public NoteResponse.NoteListDTO getNotesBySortFilter(Long userId, Integer page, Integer size, String order,
		String color) {
		User user = userRepository.findById(userId)
			.orElseThrow(() -> new BadRequestException(ErrorResponseStatus.INVALID_USERID));

		int getNotePage = (page != null) ? page : 0;
		int getNoteSize = (size != null) ? size : Integer.MAX_VALUE;
		Pageable pageable = PageRequest.of(getNotePage, getNoteSize);

		// 잘못된 order 파라미터 처리
		if (order != null && !order.isEmpty()) {
			if (!Arrays.asList("asc", "desc", "edit-newest", "edit-oldest").contains(order)) {
				throw new BadRequestException(ErrorResponseStatus.REQUEST_ERROR);
			}
		}

		Page<Note> notePage;
		// color 파라미터 주어질 때와 아닐때, order가 같이 주어질 때
		if (color != null && !color.isEmpty()) {
			List<String> colorList = Arrays.asList(color.split(","));

			// cardify 규격 색상, 잘못 입력하면 error처리
			List<String> allowedColors = Arrays.asList("blue", "ocean", "lavender", "mint", "sage", "gray", "orange",
				"coral", "rose", "plum");

			for (String c : colorList) {
				if (!allowedColors.contains(c)) {
					throw new BadRequestException(ErrorResponseStatus.REQUEST_ERROR);
				}
			}

			if (order != null && !order.isEmpty()) {
				notePage = noteRepository.findByUserColorAndSort(user, colorList, order, pageable);
			} else {
				notePage = noteRepository.findByNoteIdAndUser(user, colorList, pageable);
			}
		} else if (order != null && !order.isEmpty()) {
			notePage = noteRepository.findByUserAndSort(user, order, pageable);
		} else {
			notePage = noteRepository.findByUser(user, pageable);
		}

		if (notePage.isEmpty()) {
			throw new DatabaseException(ErrorResponseStatus.NOT_EXIST_NOTE);
		}

		List<NoteResponse.NoteInfoDTO> notes = notePage.getContent()
			.stream()
			.map(noteConverter::toNoteInfoDTO)
			.collect(Collectors.toList());

		return NoteResponse.NoteListDTO.builder()
			.noteList(notes)
			.listsize(getNoteSize)
			.currentPage(getNotePage + 1)
			.totalPage(notePage.getTotalPages())
			.totalElements(notePage.getTotalElements())
			.isFirst(notePage.isFirst())
			.isLast(notePage.isLast())
			.build();
	}
}