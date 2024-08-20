package com.umc.cardify.dto.user;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.*;
import org.apache.commons.validator.routines.UrlValidator;

public class UserRequest {

    @Getter
    @Builder
    @Schema(title = "USER_REQ_01 : 회원 가입 요청 DTO")
    public static class signUp {
        @NotBlank(message = "이름을 입력해 주세요")
        @Pattern(regexp = "^[ㄱ-ㅎ가-힣a-z]{2,10}$", message = "이름은 숫자 및 특수문자를 제외한 2~10 자리여야 합니다.")
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
        @Schema(description = "사용자 이메일", example = "leegardenleegarden@gmail.com")
        private String email;

        @NotBlank(message = "비밀번호를 입력해 주세요.")
        @Schema(description = "비밀 번호", example = "leegarden123!")
        @Pattern(regexp="(?=.*[0-9])(?=.*[a-zA-Z])(?=.*\\W)(?=\\S+$).{8,20}",
                message = "비밀번호는 영문 대,소문자와 숫자, 특수기호가 적어도 1개 이상씩 포함된 8자 ~ 20자의 비밀번호여야 합니다.")
        private String password;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(title = "USER_REQ_03 : 프로필 이미지 수정 요청 DTO")
    public static class UpdateProfileImage {
        @NotBlank(message = "프로필 이미지 URL을 입력해 주세요.")
        @Schema(description = "수정할 프로필 이미지 URL", example = "https://example.com/profile.jpg")
        private String profileImage;

        @AssertTrue(message = "올바른 URL 형식이 아닙니다.")
        private boolean isValidUrl() {
            UrlValidator urlValidator = new UrlValidator(new String[]{"http", "https"});
            return urlValidator.isValid(profileImage);
        }

    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(title = "USER_REQ_04 : 이름 수정 요청 DTO")
    public static class UpdateName {
        @NotBlank(message = "이름을 입력해 주세요")
        @Pattern(regexp = "^[ㄱ-ㅎ가-힣a-z]{2,10}$", message = "이름은 숫자 및 특수문자를 제외한 2~10 자리여야 합니다.")
        @Schema(description = "수정할 사용자 이름", example = "이정원")
        private String name;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(title = "USER_REQ_05 : 알림 설정 수정 요청 DTO")
    public static class UpdateNotification {
        @NotNull(message = "알림 설정 여부를 true 혹은 false로 입력해 주세요.")
        @Schema(description = "알림 설정 여부 (true 또는 false만 가능)", example = "true", allowableValues = {"true", "false"})
        private Boolean notificationEnabled;

        @AssertTrue(message = "알림 설정은 true 또는 false만 가능합니다.")
        private boolean isValidNotificationSetting() {
            return notificationEnabled != null;
        }
    }

}
