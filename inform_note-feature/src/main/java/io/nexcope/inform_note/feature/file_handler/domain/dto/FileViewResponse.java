package io.nexcope.inform_note.feature.file_handler.domain.dto;

import io.nexcope.inform_note.domain.file.entity.AttachedFile;
import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.core.io.Resource;

@Schema(description = "파일 스트리밍 리소스 및 메타데이터 DTO")
public record FileViewResponse(
        //
        @Schema(description = "물리 저장소 파일 바이너리 스트리밍 리소스")
        Resource resource,
        @Schema(description = "첨부파일 메타데이터 정보 (DB 엔티티)")
        AttachedFile file
) {
}

