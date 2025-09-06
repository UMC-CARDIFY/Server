package com.umc.cardify.util;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;

import java.io.IOException;

public final class NotePreviewUtil {
    private static final JsonFactory FACTORY = new JsonFactory();

    private NotePreviewUtil() {}

    /**
     * JSON에서 미리보기 평문을 추출하고, 필요하면 "..."을 붙여 반환
     *
     * @param json   ProseMirror/Tiptap 형식의 JSON
     * @param maxLen 최대 표시할 코드포인트 수 (예: 300)
     */
    public static String extractPreview(String json, int maxLen) {
        if (json == null || json.isBlank()) return "";

        StringBuilder sb = new StringBuilder();
        boolean truncatedDuringParse = false;

        // softLimit: 정규화 전 충분한 데이터 확보를 위해 maxLen * 3 정도 허용
        final int softLimit = Math.max(1024, maxLen * 3);

        try (JsonParser parser = FACTORY.createParser(json)) {
            String currentField = null;
            while (!parser.isClosed()) {
                JsonToken token = parser.nextToken();
                if (token == null) break;

                if (token == JsonToken.FIELD_NAME) {
                    currentField = parser.getCurrentName();
                } else if (token == JsonToken.VALUE_STRING) {
                    // 카드 블록의 경우 관심있는 텍스트만 출력
                    if ("text".equals(currentField)
                            || "question_front".equals(currentField)
                            || "question_back".equals(currentField)) {
                        sb.append(parser.getValueAsString()).append(' ');
                    }
                } else if (token == JsonToken.START_ARRAY && "answer".equals(currentField)) {
                    // answer 배열 안의 문자열들
                    while (parser.nextToken() != JsonToken.END_ARRAY) {
                        if (parser.getCurrentToken() == JsonToken.VALUE_STRING) {
                            sb.append(parser.getValueAsString()).append(' ');
                        }
                    }
                }

                // 성능안: softLimit을 넘어가면 파싱 중단(너무 커지면 안됨)
                if (sb.length() > softLimit) {
                    truncatedDuringParse = true;
                    break;
                }
            }
        } catch (IOException e) {
            // 파싱실패 시 빈 문자열 반환 (원하면 로깅 추가)
            return "";
        }

        // 공백/개행 정리
        String normalized = sb.toString().replaceAll("\\s+", " ").trim();

        // 유니코드(코드포인트) 안전하게 길이 계산
        int cpCount = normalized.codePointCount(0, normalized.length());

        // 잘렸는지 판단: 정규화 후 길이가 초과되었거나 파싱 중 조기 중단(truncatedDuringParse)
        if (truncatedDuringParse || cpCount > maxLen) {
            int cpToKeep = Math.min(maxLen, cpCount);
            int endIdx = normalized.offsetByCodePoints(0, cpToKeep);
            return normalized.substring(0, endIdx).stripTrailing() + "...";
        } else {
            return normalized;
        }
    }
}