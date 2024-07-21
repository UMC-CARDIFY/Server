package com.umc.cardify.config.exception;

import lombok.Getter;

@Getter
public class AwsS3Exception extends RuntimeException {

	private final ErrorResponseStatus status;

	public AwsS3Exception(ErrorResponseStatus status) {
		this.status = status;
	}
}

