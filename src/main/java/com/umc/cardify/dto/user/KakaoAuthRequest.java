package com.umc.cardify.dto.user;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
@Schema(title = "KAKAO_AUTH_01 : 카카오 로그인 인가 코드 요청 DTO")
public class KakaoAuthRequest {

    @NotBlank(message = "인가 코드를 입력해 주세요.")
    @Schema(description = "카카오 인가 코드", example = "4/P7q7W91a-oBNn5y2mRo92bp5FmXcO1qYCI")
    private String code;

}
