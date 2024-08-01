package com.umc.cardify.dto.user;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import org.springframework.context.annotation.ComponentScan;


public class UserResponse {

	@Getter
	@Builder
	@Schema(title = "USER_RES_01 : 로그인 응답 DTO (토큰)")
	public static class tokenInfo {
		@Schema(description = "토큰의 타입")
		private String grantType;

		@Schema(description = "액세스 토큰")
		private String accessToken;

		@Schema(description = "리프레시 토큰")
		private String refreshToken;
	}

}
