package com.umc.cardify.service.auth;

import com.umc.cardify.dto.auth.AuthResponse;
import jakarta.servlet.http.HttpServletRequest;

public interface AuthService {

  AuthResponse.RefreshTokenRes refreshAccessToken(HttpServletRequest request);
  AuthResponse.CheckRefreshTokenRes checkRefreshToken(HttpServletRequest request);
}