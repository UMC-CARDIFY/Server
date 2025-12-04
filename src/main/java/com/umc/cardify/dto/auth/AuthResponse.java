package com.umc.cardify.dto.auth;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

public class AuthResponse {

  @Builder
  @Schema(title = "AUTH_RES_01 : 액세스 토큰 갱신 응답")
  public record RefreshTokenRes(
      @Schema(description = "새로 발급된 액세스 토큰", example = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...")
      String accessToken
  ) {}

  @Builder
  @Schema(title = "AUTH_RES_02 : 리프레시 토큰 유효성 검증 응답")
  public record CheckRefreshTokenRes(
      @Schema(description = "리프레시 토큰 유효 여부", example = "true")
      boolean valid
  ) {}
}