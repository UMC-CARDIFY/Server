package com.umc.cardify.dto.user;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

public class UserRequest {

    @Getter
    @Builder
    @Schema(title = "USER_REQ_01 : 회원 가입 요청 DTO")
    public static class signUp {
        @NotBlank(message = "이름을 입력해 주세요")
        @Pattern(regexp = "^[ㄱ-ㅎ가-힣a-z0-9-_]{2,10}$", message = "이름은 특수문자를 제외한 2~12 자리여야 합니다.")
        @Schema(description = "사용자 이름", example = "홍길동")
        private String name;

        @NotBlank(message = "이메일을 입력해 주세요.")
        @Email(message = "이메일 형식에 맞지 않습니다.")
        @Schema(description = "사용자 이메일", example = "testtest@gmail.com")
        private String email;

        @NotBlank(message = "비밀번호를 입력해 주세요.")
        @Schema(description = "비밀 번호", example = "test123!")
        @Pattern(regexp="(?=.*[0-9])(?=.*[a-zA-Z])(?=.*\\W)(?=\\S+$).{8,20}",
                message = "비밀번호는 영문 대,소문자와 숫자, 특수기호가 적어도 1개 이상씩 포함된 8자 ~ 20자의 비밀번호여야 합니다.")
        private String password;
    }


    @Getter
    @AllArgsConstructor
    @Schema(title = "USER_REQ_02 : 로그인 요청 DTO")
    public static class login {
        @NotBlank(message = "아이디를 입력해 주세요.")
        @Email(message = "이메일 형식에 맞지 않습니다.")
        @Schema(description = "사용자 이메일", example = "testtest@gmail.com")
        private String email;

        @NotBlank(message = "비밀번호를 입력해 주세요.")
        @Schema(description = "비밀 번호", example = "test123!")
        @Pattern(regexp="(?=.*[0-9])(?=.*[a-zA-Z])(?=.*\\W)(?=\\S+$).{8,20}",
                message = "비밀번호는 영문 대,소문자와 숫자, 특수기호가 적어도 1개 이상씩 포함된 8자 ~ 20자의 비밀번호여야 합니다.")
        private String password;
    }

}
