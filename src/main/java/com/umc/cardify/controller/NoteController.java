package com.umc.cardify.controller;

import java.util.List;

import com.umc.cardify.auth.jwt.JwtTokenProvider;
import com.umc.cardify.config.exception.BadRequestException;
import com.umc.cardify.config.exception.ErrorResponseStatus;
import com.umc.cardify.domain.User;
import com.umc.cardify.domain.enums.AuthProvider;
import com.umc.cardify.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import com.umc.cardify.converter.NoteConverter;
import com.umc.cardify.domain.Folder;
import com.umc.cardify.domain.Note;
import com.umc.cardify.dto.note.NoteRequest;
import com.umc.cardify.dto.note.NoteResponse;
import com.umc.cardify.service.FolderService;
import com.umc.cardify.service.NoteComponentService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@Tag(name = "NoteController", description = "노트 관련 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("api/v1/notes")
public class NoteController {

	private final FolderService folderService;
	private final NoteComponentService noteComponentService;
	private final JwtTokenProvider jwtTokenProvider;
	private final UserRepository userRepository;

	@GetMapping("/addNote")
	@Operation(summary = "노트 추가 API")
	public ResponseEntity<NoteResponse.AddNoteResultDTO> addNote(@RequestHeader("Authorization") String token,
		@RequestParam @Valid Long folderId) {
		String email = jwtTokenProvider.getEmailFromToken(token.replace("Bearer ", ""));
		AuthProvider provider = jwtTokenProvider.getProviderFromToken(token.replace("Bearer ", "")); // 토큰에 제공자 정보도 포함
		Long userId = userRepository.findByEmailAndProvider(email, provider)
				.orElseThrow(() -> new BadRequestException(ErrorResponseStatus.INVALID_USERID)).getUserId();

		if(!noteComponentService.checkNoteCnt(userId))
			throw new BadRequestException(ErrorResponseStatus.NOTE_CREATED_NOT_ALLOWED);

		Folder folder = folderService.getFolder(folderId);
		Note note = noteComponentService.addNote(folder, userId);
		return ResponseEntity.ok(NoteConverter.toAddNoteResult(note));
	}

	@DeleteMapping("/deleteNote")
	@Operation(summary = "노트 삭제 API", description = "노트 ID 입력, 성공 시 삭제 성공 여부 반환")
	public ResponseEntity<NoteResponse.IsSuccessNoteDTO> deleteNote(@RequestHeader("Authorization") String token,
		@RequestParam @Valid Long noteId) {
		String email = jwtTokenProvider.getEmailFromToken(token.replace("Bearer ", ""));
		AuthProvider provider = jwtTokenProvider.getProviderFromToken(token.replace("Bearer ", "")); // 토큰에 제공자 정보도 포함
		Long userId = userRepository.findByEmailAndProvider(email, provider)
				.orElseThrow(() -> new BadRequestException(ErrorResponseStatus.INVALID_USERID)).getUserId();

		Boolean isSuccess = noteComponentService.deleteNote(noteId, userId);
		return ResponseEntity.ok(NoteConverter.isSuccessNoteResult(isSuccess));
	}

	// TODO : auth 인증 추가 여부
	@PostMapping("/getNoteToFolder")
	@Operation(summary = "특정 폴더 내 노트 조회 API", description =
		"폴더 ID 입력, 성공 시 노트 리스트 반환 | order = asc, desc, edit-newest, edit-oldest, create-newest, create-oldest |"
			+ " 페이지 번호, 사이즈 미입력시 페이징 X | 정렬방식 미입력시 이름 오름차순")
	public ResponseEntity<NoteResponse.GetNoteToFolderResultDTO> getNoteToFolder(
		@RequestBody @Valid NoteRequest.GetNoteToFolderDto request) {
		Folder folder = folderService.getFolder(request.getFolderId());
		NoteResponse.GetNoteToFolderResultDTO noteList = noteComponentService.getNoteToFolder(folder, request);
		return ResponseEntity.ok(noteList);
	}

	@GetMapping("/markNote")
	@Operation(summary = "노트 즐겨찾기 API", description = "노트 ID와 즐겨찾기 여부 입력, 성공 시 즐겨찾기 성공 여부 반환")
	public ResponseEntity<NoteResponse.IsSuccessNoteDTO> markNote(@RequestHeader("Authorization") String token,
		@RequestParam @Valid Long noteId, @RequestParam @Valid Boolean isMark) {
		String email = jwtTokenProvider.getEmailFromToken(token.replace("Bearer ", ""));
		AuthProvider provider = jwtTokenProvider.getProviderFromToken(token.replace("Bearer ", "")); // 토큰에 제공자 정보도 포함
		Long userId = userRepository.findByEmailAndProvider(email, provider)
				.orElseThrow(() -> new BadRequestException(ErrorResponseStatus.INVALID_USERID)).getUserId();

		Boolean isSuccess = noteComponentService.markNote(noteId, isMark, userId);
		return ResponseEntity.ok(NoteConverter.isSuccessNoteResult(isSuccess));
	}

	@PostMapping(value = "/write", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	@Operation(summary = "노트 작성 API", description = "노트 UUID 입력, 성공 시 작성 성공 여부 반환")
	public ResponseEntity<NoteResponse.IsSuccessNoteDTO> writeNote(@RequestHeader("Authorization") String token,
		@RequestPart(value = "images", required = false) List<MultipartFile> images,
		@RequestPart @Valid NoteRequest.WriteNoteDto request) {
		String email = jwtTokenProvider.getEmailFromToken(token.replace("Bearer ", ""));
		AuthProvider provider = jwtTokenProvider.getProviderFromToken(token.replace("Bearer ", "")); // 토큰에 제공자 정보도 포함
		Long userId = userRepository.findByEmailAndProvider(email, provider)
				.orElseThrow(() -> new BadRequestException(ErrorResponseStatus.INVALID_USERID)).getUserId();

		Boolean isSuccess = noteComponentService.writeNote(request, userId, images);
		return ResponseEntity.ok(NoteConverter.isSuccessNoteResult(isSuccess));
	}

	// TODO : auth 추가 여부
	@PostMapping("/searchFolder")
	@Operation(summary = "폴더 내 노트 검색 API", description = "폴더 ID와 검색어 입력, 성공 시 검색 결과 반환")
	public ResponseEntity<NoteResponse.SearchNoteDTO> searchNote(
		@RequestBody @Valid NoteRequest.SearchNoteDto request) {
		Folder folder = folderService.getFolder(request.getFolderId());
		String searchTxt = request.getSearchTxt();
		List<NoteResponse.SearchNoteResDTO> DTOList = noteComponentService.searchNote(folder, searchTxt);
		return ResponseEntity.ok(NoteResponse.SearchNoteDTO.builder().searchTxt(searchTxt).noteList(DTOList).build());
	}

	@GetMapping("/searchAll")
	@Operation(summary = "전체 노트 검색 API", description = "검색어 입력, 성공 시 검색 결과 반환")
	public ResponseEntity<NoteResponse.SearchNoteAllDTO> searchNoteAll(@RequestHeader("Authorization") String token,
			@RequestParam @Valid String search) {
		String email = jwtTokenProvider.getEmailFromToken(token.replace("Bearer ", ""));
		AuthProvider provider = jwtTokenProvider.getProviderFromToken(token.replace("Bearer ", "")); // 토큰에 제공자 정보도 포함
		User user = userRepository.findByEmailAndProvider(email, provider)
				.orElseThrow(() -> new BadRequestException(ErrorResponseStatus.INVALID_USERID));

		NoteResponse.SearchNoteAllDTO dto = noteComponentService.searchNoteAll(user, search);
		noteComponentService.addSearchHistory(user, search);

		return ResponseEntity.ok(dto);
	}

	@PostMapping("/shareLib")
	@Operation(summary = "노트 자료실 업로드 API", description = "노트 아이디 입력, 성공 시 자료실 저장 성공 여부")
	public ResponseEntity<NoteResponse.IsSuccessNoteDTO> shareLib(@RequestHeader("Authorization") String token,
		@RequestBody @Valid NoteRequest.ShareLibDto request) {
		String email = jwtTokenProvider.getEmailFromToken(token.replace("Bearer ", ""));
		AuthProvider provider = jwtTokenProvider.getProviderFromToken(token.replace("Bearer ", "")); // 토큰에 제공자 정보도 포함
		Long userId = userRepository.findByEmailAndProvider(email, provider)
				.orElseThrow(() -> new BadRequestException(ErrorResponseStatus.INVALID_USERID)).getUserId();

		Boolean isSuccess = noteComponentService.shareLib(userId, request);
		return ResponseEntity.ok(NoteConverter.isSuccessNoteResult(isSuccess));
	}

	@DeleteMapping("/cancelShare")
	@Operation(summary = "노트 공유 취소 API", description = "노트 ID 입력, 성공 시 즐겨찾기 성공 여부 반환")
	public ResponseEntity<NoteResponse.IsSuccessNoteDTO> cancelShare(@RequestHeader("Authorization") String token,
		@RequestParam @Valid Long noteId) {
		String email = jwtTokenProvider.getEmailFromToken(token.replace("Bearer ", ""));
		AuthProvider provider = jwtTokenProvider.getProviderFromToken(token.replace("Bearer ", "")); // 토큰에 제공자 정보도 포함
		Long userId = userRepository.findByEmailAndProvider(email, provider)
				.orElseThrow(() -> new BadRequestException(ErrorResponseStatus.INVALID_USERID)).getUserId();

		Boolean isSuccess = noteComponentService.cancelShare(noteId, userId);
		return ResponseEntity.ok(NoteConverter.isSuccessNoteResult(isSuccess));
	}

	@GetMapping("/getNote")
	@Operation(summary = "노트 내용 조회 API", description = "노트 ID 입력, 성공 시 노트 내용 반환")
	public ResponseEntity<NoteResponse.getNoteDTO> getNote(@RequestParam @Valid Long noteId) {
		return ResponseEntity.ok(noteComponentService.getNote(noteId));
	}

	@GetMapping("/recent-notes")
	@Operation(summary = "최신 열람 노트 조회 API", description = "사용자의 최신 열람 노트 4개/3개 반환")
	public ResponseEntity<List<NoteResponse.NoteInfoDTO>> gerRecentNote(@RequestHeader("Authorization") String token,
		@RequestParam(required = false, defaultValue = "0") int page, @RequestParam(required = false) Integer size) {
		String email = jwtTokenProvider.getEmailFromToken(token.replace("Bearer ", ""));
		AuthProvider provider = jwtTokenProvider.getProviderFromToken(token.replace("Bearer ", "")); // 토큰에 제공자 정보도 포함
		Long userId = userRepository.findByEmailAndProvider(email, provider)
				.orElseThrow(() -> new BadRequestException(ErrorResponseStatus.INVALID_USERID)).getUserId();

		List<NoteResponse.NoteInfoDTO> notes = noteComponentService.getRecentNotes(userId, page, size);
		return ResponseEntity.ok(notes);
	}

	@GetMapping("/recent-search")
	@Operation(summary = "최근 검색어 조회 API", description = "사용자의 최근 검색어 최대 5개 반환")
	public ResponseEntity<List<String>> getRecentSearch(@RequestHeader("Authorization") String token) {
		String email = jwtTokenProvider.getEmailFromToken(token.replace("Bearer ", ""));
		AuthProvider provider = jwtTokenProvider.getProviderFromToken(token.replace("Bearer ", "")); // 토큰에 제공자 정보도 포함
		User user = userRepository.findByEmailAndProvider(email, provider)
				.orElseThrow(() -> new BadRequestException(ErrorResponseStatus.INVALID_USERID));

		return ResponseEntity.ok(noteComponentService.getSearchHistory(user));
	}

	@DeleteMapping("/recent-search")
	@Operation(summary = "최근 검색어 삭제 API")
	public ResponseEntity<NoteResponse.IsSuccessNoteDTO> delRecentSearch(@RequestHeader("Authorization") String token, @RequestParam @Valid String search) {
		String email = jwtTokenProvider.getEmailFromToken(token.replace("Bearer ", ""));
		AuthProvider provider = jwtTokenProvider.getProviderFromToken(token.replace("Bearer ", "")); // 토큰에 제공자 정보도 포함
		User user = userRepository.findByEmailAndProvider(email, provider)
				.orElseThrow(() -> new BadRequestException(ErrorResponseStatus.INVALID_USERID));

		return ResponseEntity.ok(NoteConverter.isSuccessNoteResult(noteComponentService.delSearchHistory(user, search)));
	}

	@PostMapping("/link")
	@Operation(summary = "노트 링크 생성 API", description = "노트 아이디 입력, 성공 시 노트 고유값 반환")
	public ResponseEntity<NoteResponse.getNoteUUIDDTO> createNoteUUID(@RequestHeader("Authorization") String token, @RequestBody @Valid NoteRequest.MakeLinkDto request) {
		String email = jwtTokenProvider.getEmailFromToken(token.replace("Bearer ", ""));
		AuthProvider provider = jwtTokenProvider.getProviderFromToken(token.replace("Bearer ", "")); // 토큰에 제공자 정보도 포함
		User user = userRepository.findByEmailAndProvider(email, provider)
				.orElseThrow(() -> new BadRequestException(ErrorResponseStatus.INVALID_USERID));

		return ResponseEntity.ok(noteComponentService.createNoteUUID(request, user));
	}

	@GetMapping("/link")
	@Operation(summary = "노트 링크 조회 API", description = "고유값 입력받아 노트 내용 전달")
	public ResponseEntity<NoteResponse.getNoteDTO> getNoteToUUID(@RequestParam @Valid String uuid) {
		return ResponseEntity.ok(noteComponentService.getNote(noteComponentService.getNoteIdToUUID(uuid)));
	}

	@DeleteMapping("/link")
	@Operation(summary = "노트 링크 삭제 API")
	public ResponseEntity<NoteResponse.IsSuccessNoteDTO> delNoteLink(@RequestHeader("Authorization") String token, @RequestParam @Valid Long noteId) {
		String email = jwtTokenProvider.getEmailFromToken(token.replace("Bearer ", ""));
		AuthProvider provider = jwtTokenProvider.getProviderFromToken(token.replace("Bearer ", "")); // 토큰에 제공자 정보도 포함
		User user = userRepository.findByEmailAndProvider(email, provider)
				.orElseThrow(() -> new BadRequestException(ErrorResponseStatus.INVALID_USERID));

		return ResponseEntity.ok(NoteConverter.isSuccessNoteResult(noteComponentService.delNoteUUID(user, noteId)));
	}

	@GetMapping("/recent-marks")
	@Operation(summary = "최근 즐겨찾기한 노트 목록 조회 API", description = "최근 즐겨찾기한 순으로 조회 | 최대 3개")
	public ResponseEntity<List<NoteResponse.RecentNoteDTO>> getRecentFavoriteNotes(
			@RequestHeader("Authorization") String token) {
		String email = jwtTokenProvider.getEmailFromToken(token.replace("Bearer ", ""));
		AuthProvider provider = jwtTokenProvider.getProviderFromToken(token.replace("Bearer ", "")); // 토큰에 제공자 정보도 포함
		User user = userRepository.findByEmailAndProvider(email, provider)
				.orElseThrow(() -> new BadRequestException(ErrorResponseStatus.INVALID_USERID));
		return ResponseEntity.ok(noteComponentService.getRecentFavoriteNotes(user.getUserId()));
	}
}
