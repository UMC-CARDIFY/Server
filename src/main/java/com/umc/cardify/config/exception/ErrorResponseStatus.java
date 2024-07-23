package com.umc.cardify.config.exception;

import lombok.Getter;

@Getter
public enum ErrorResponseStatus {
	// 2000 : Request 오류
	REQUEST_ERROR(2000, "입력값을 확인 해주세요."),
	DUPLICATE_ERROR(2001, "중복된 레코드 입니다."),
	INVALID_PWD(2002, "비밀번호가 올바르지 않습니다."),
	INVALID_USERID(2003, "유효하지 않는 USERID 입니다."),

	// 3000 : Response 오류
	RESPONSE_ERROR(3000, "값을 불러오는데 실패하였습니다."),

	//4000 : Database, Server 오류
	DATABASE_ERROR(4000, "데이터 베이스 접근 오류."),
	QUERY_TIMEOUT_ERROR(4001, "쿼리 타임 아웃 에러."),
	DB_INSERT_ERROR(4002, "DB에 값을 삽입 하는데 실패 했습니다."),
	NOT_FOUND_ERROR(4003, "해당 레코드가 존재하지 않습니다."),
	IMAGE_DELETE_ERROR(4004, "이미지 삭제 실패"),
	IMAGE_UPLOAD_ERROR(4005, "이미지 업로드 실패"),
	DB_UPDATE_ERROR(4006, "DB에 값을 수정 하는데 실패 했습니다."),

	//5000 : Server connection 오류
	SERVER_ERROR(5000, "서버와의 연결에 실패하였습니다."),

	;

	private final int code;
	private final String message;

	private ErrorResponseStatus(int code, String message) {
		this.code = code;
		this.message = message;
	}

}

