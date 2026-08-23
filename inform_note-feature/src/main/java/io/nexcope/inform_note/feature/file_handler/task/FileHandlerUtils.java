package io.nexcope.inform_note.feature.file_handler.task;

import org.springframework.stereotype.Component;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class FileHandlerUtils {
    // HTML 태그 내 파일 ID 및 파일명 추출용 정규식 패턴들
    // 1) /feature/file-handler/(view|download|image)/{fileId} 및 /api/files/(view|download)/{fileId} 패턴
    private static final Pattern URL_FILE_KEY_PATTERN = Pattern.compile("/(?:feature/file-handler|api/files)/(?:view|download|image|view-image)/([a-zA-Z0-9_.-]+)");
    // 2) data-file-id="{fileId}" 속성 패턴
    private static final Pattern DATA_FILE_ID_PATTERN = Pattern.compile("data-file-id=[\"']([a-zA-Z0-9_.-]+)[\"']");
    // 3) data-file-name="{storedFileName}" 속성 패턴
    private static final Pattern DATA_FILE_NAME_PATTERN = Pattern.compile("data-file-name=[\"']([a-zA-Z0-9_.-]+)[\"']");


    /**
     * HTML 문자열에서 이미지 URL, data-file-id, data-file-name 속성에 포함된 파일 키(ID 또는 저장파일명)들을 추출합니다.
     *
     * @param contentHtml HTML 본문
     * @return 추출된 파일 식별자 Set
     */
    public static Set<String> extractFileKeysFromHtml(String contentHtml) {
        Set<String> keys = new LinkedHashSet<>();
        if (contentHtml == null || contentHtml.trim().isEmpty()) {
            return keys;
        }

        // 1. URL 패턴 매칭 (/api/files/view/{key} 등)
        Matcher urlMatcher = URL_FILE_KEY_PATTERN.matcher(contentHtml);
        while (urlMatcher.find()) {
            String key = urlMatcher.group(1);
            if (key != null && !key.trim().isEmpty()) {
                keys.add(key.trim());
            }
        }

        // 2. data-file-id 매칭
        Matcher idMatcher = DATA_FILE_ID_PATTERN.matcher(contentHtml);
        while (idMatcher.find()) {
            String key = idMatcher.group(1);
            if (key != null && !key.trim().isEmpty()) {
                keys.add(key.trim());
            }
        }

        // 3. data-file-name 매칭
        Matcher nameMatcher = DATA_FILE_NAME_PATTERN.matcher(contentHtml);
        while (nameMatcher.find()) {
            String key = nameMatcher.group(1);
            if (key != null && !key.trim().isEmpty()) {
                keys.add(key.trim());
            }
        }

        return keys;
    }


}
