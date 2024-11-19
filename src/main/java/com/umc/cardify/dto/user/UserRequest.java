package com.umc.cardify.dto.user;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.*;
import org.apache.commons.validator.routines.UrlValidator;

public class UserRequest {


    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(title = "USER_REQ_01 : 프로필 이미지 수정 요청 DTO")
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
    @Schema(title = "USER_REQ_02 : 이름 수정 요청 DTO")
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
    @Schema(title = "USER_REQ_03 : 알림 설정 수정 요청 DTO")
    public static class UpdateNotification {
        @NotNull(message = "알림 설정 여부를 true 혹은 false로 입력해 주세요.")
        @Schema(description = "알림 설정 여부 (true 또는 false만 가능)", example = "true", allowableValues = {"true", "false"})
        private Boolean notificationEnabled;

        public boolean getNotificationEnabled() {
            return notificationEnabled;
        }

    }

}
