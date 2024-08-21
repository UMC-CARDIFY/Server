package com.umc.cardify.dto.user;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

public class UserResponse {

	@Getter
	@Builder
	@NoArgsConstructor
	@AllArgsConstructor
	@Schema(title = "USER_RES_01 : 로그인 응답 DTO (토큰)")
	public static class tokenInfo {
		@Schema(description = "토큰의 타입")
		private String grantType;

		@Schema(description = "액세스 토큰")
		private String accessToken;

		@Schema(description = "리프레시 토큰")
		private String refreshToken;
	}

	@Getter
	@Builder
	@NoArgsConstructor
	@AllArgsConstructor
	@Schema(title = "USER_RES_02 : 마이페이지 정보 응답 DTO")
	public static class MyPageInfo {
		@Schema(description = "사용자 ID")
		private Long userId;

		@Schema(description = "사용자 이름")
		private String name;

		@Schema(description = "사용자 이메일")
		private String email;

		@Schema(description = "프로필 이미지 URL")
		private String profileImage;

		@Schema(description = "포인트")
		private Integer point;

		@Schema(description = "알림 설정 여부")
		private boolean notificationEnabled;

		@Schema(description = "리프래시 토큰")
		private String refreshToken;

		@Schema(description = "당일 출석 체크 여부 ( 0 = 미 완료 , 1 = 완료 )")
		private int todayCheck;
	}

	@Getter
	@Builder
	@NoArgsConstructor
	@AllArgsConstructor
	@Schema(title = "USER_RES_03 : 프로필 이미지 수정 응답 DTO")
	public static class UpdatedProfileImage {
		@Schema(description = "수정된 프로필 이미지 URL")
		private String profileImage;
	}

	@Getter
	@Builder
	@NoArgsConstructor
	@AllArgsConstructor
	@Schema(title = "USER_RES_04 : 이름 수정 응답 DTO")
	public static class UpdatedName {
		@Schema(description = "수정된 사용자 이름")
		private String name;
	}

	@Getter
	@Builder
	@NoArgsConstructor
	@AllArgsConstructor
	@Schema(title = "USER_RES_05 : 알림 설정 수정 응답 DTO")
	public static class UpdatedNotification {
		@Schema(description = "수정된 알림 설정 여부")
		private boolean notificationEnabled;
	}

	@Getter
	@Builder
	@NoArgsConstructor
	@AllArgsConstructor
	@Schema(title = "USER_RES_06 : 로그아웃 응답 DTO")
	public static class LogoutResponse {
		@Schema(description = "로그아웃 상태 메시지")
		private String message;

		@Schema(description = "로그아웃 성공 여부")
		private boolean success;
	}
}