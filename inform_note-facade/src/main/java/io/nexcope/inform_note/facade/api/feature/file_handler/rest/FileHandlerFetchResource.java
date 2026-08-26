package io.nexcope.inform_note.facade.api.feature.file_handler.rest;

import io.nexcope.inform_note.domain.file.entity.AttachedFile;
import io.nexcope.inform_note.feature.file_handler.domain.dto.FileViewResponse;
import io.nexcope.inform_note.feature.file_handler.flow.FileHandlerFetch;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;

@Tag(name = "FileHandler", description = "현상 및 조치 내용 의 파일 조회 API")
@RestController
@RequestMapping("/feature/file-handler")
@RequiredArgsConstructor
public class FileHandlerFetchResource {
    //
    private final FileHandlerFetch fileHandlerFetch;

    @Operation(summary = "이미지/파일 인라인 조회 (에디터 <img> 태그용)")
    @GetMapping("/view/{fileId}")
    public ResponseEntity<Resource> viewFile(@PathVariable("fileId") String fileId) {
        // 1. Feature 계층을 통해 파일 메타데이터 및 물리 리소스 로드
        FileViewResponse viewResource = fileHandlerFetch.loadViewResource(fileId);
        if (viewResource == null) {
            return ResponseEntity.notFound().build();
        }

        AttachedFile file = viewResource.file();
        Resource resource = viewResource.resource();

        // 2. 브라우저 화면 렌더링 헤더 설정 (inline)
        String contentDisposition = ContentDisposition.inline()
                .filename(file.getOriginFileName(), StandardCharsets.UTF_8)
                .build()
                .toString();

        // 3. 스트리밍 응답 반환
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(file.getContentType())) // image/png, image/jpeg 등
                .header(HttpHeaders.CONTENT_DISPOSITION, contentDisposition)
                .header(HttpHeaders.CACHE_CONTROL, "public, max-age=86400, must-revalidate") // 캐싱
                .header("X-Content-Type-Options", "nosniff") // MIME 스니핑 방지 보안 헤더
                .body(resource);
    }
}

