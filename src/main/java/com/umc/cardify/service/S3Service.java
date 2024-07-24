package com.umc.cardify.service;

import static com.umc.cardify.config.exception.ErrorResponseStatus.*;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.CannedAccessControlList;
import com.amazonaws.services.s3.model.DeleteObjectRequest;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.umc.cardify.config.exception.AwsS3Exception;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class S3Service {

	private final AmazonS3 amazonS3Client;

	private final AmazonS3 s3Client;
	@Value("${cloud.aws.s3.bucket}")
	private String bucket;

	public String upload(MultipartFile multipartFile, String dirName) {
		if (multipartFile.getSize() <= 0) {
			log.debug("Empty file received, upload skipped");
			return null;
		}

		log.debug("Attempting to upload file [{}] to directory [{}]", multipartFile.getOriginalFilename(), dirName);

		File uploadFile = null;
		try {
			uploadFile = convert(multipartFile)
				.orElseThrow(() -> new IllegalArgumentException("Failed to convert MultipartFile to File"));

			return uploadToS3(uploadFile, dirName);
		} catch (IOException e) {
			log.error("Error uploading file to S3", e);
			throw new AwsS3Exception(IMAGE_UPLOAD_ERROR);
		} finally {
			if (uploadFile != null && !uploadFile.delete()) {
				log.error("Failed to delete temporary file [{}]", uploadFile.getPath());
			}
		}
	}

	private String uploadToS3(File uploadFile, String dirName) {
		String fileName = dirName + "/" + uploadFile.getName();
		log.debug("Uploading file [{}] to S3 bucket [{}]", fileName, bucket);
		amazonS3Client.putObject(
			new PutObjectRequest(bucket, fileName, uploadFile)
				.withCannedAcl(CannedAccessControlList.PublicRead)
		);
		String fileUrl = amazonS3Client.getUrl(bucket, fileName).toString();
		log.info("File [{}] uploaded successfully to URL [{}]", fileName, fileUrl);
		return fileUrl;
	}

	private Optional<File> convert(MultipartFile file) throws IOException {
		File convertFile = new File(System.getProperty("java.io.tmpdir") + "/" + file.getOriginalFilename());
		log.debug("Creating temporary file [{}] for conversion", convertFile.getPath());

		if (convertFile.createNewFile()) {
			try (FileOutputStream fos = new FileOutputStream(convertFile)) {
				fos.write(file.getBytes());
				log.info("Temporary file [{}] created successfully", convertFile.getPath());
				return Optional.of(convertFile);
			} catch (IOException e) {
				log.error("Failed to write to temporary file [{}]", convertFile.getPath(), e);
				throw e;
			}
		} else {
			log.error("Failed to create new file [{}]", convertFile.getPath());
			return Optional.empty();
		}
	}

	// 여러개의 파일 업로드
	public List<String> uploadFileList(List<MultipartFile> multipartFile) {
		List<String> fileNameList = new ArrayList<>();

		multipartFile.forEach(file -> {
			String fileName = createFileName(file.getOriginalFilename());
			ObjectMetadata objectMetadata = new ObjectMetadata();
			objectMetadata.setContentLength(file.getSize());
			objectMetadata.setContentType(file.getContentType());

			try (InputStream inputStream = file.getInputStream()) {
				s3Client.putObject(new PutObjectRequest(bucket, fileName, inputStream, objectMetadata).withCannedAcl(
					CannedAccessControlList.PublicRead));
			} catch (IOException e) {
				throw new AwsS3Exception(IMAGE_UPLOAD_ERROR);
			}

			fileNameList.add(fileName);
		});

		return fileNameList;
	}

	// 파일 삭제
	public void deleteFile(String fileName) {
		s3Client.deleteObject(new DeleteObjectRequest(bucket, fileName));
	}

	// 파일명 중복 방지 (UUID)
	private String createFileName(String fileName) {
		return UUID.randomUUID().toString().concat(getFileExtension(fileName));
	}

	// 파일 유효성 검사
	private String getFileExtension(String fileName) {
		if (fileName.isEmpty()) {
			throw new AwsS3Exception(FILE_VALID_ERROR);
		}
		ArrayList<String> fileValidate = new ArrayList<>();
		fileValidate.add(".jpg");
		fileValidate.add(".jpeg");
		fileValidate.add(".png");
		fileValidate.add(".JPG");
		fileValidate.add(".JPEG");
		fileValidate.add(".PNG");
		String idxFileName = fileName.substring(fileName.lastIndexOf("."));
		if (!fileValidate.contains(idxFileName)) {
			throw new AwsS3Exception(FILE_FORMAT_ERROR);
		}
		return fileName.substring(fileName.lastIndexOf("."));
	}
}
