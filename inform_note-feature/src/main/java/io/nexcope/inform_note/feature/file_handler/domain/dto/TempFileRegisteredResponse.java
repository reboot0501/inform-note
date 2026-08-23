package io.nexcope.inform_note.feature.file_handler.domain.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "임시 파일 업로드 응답 DTO")
public record TempFileRegisteredResponse(
        @Schema(description = "생성된 파일 고유 UUID", example = "95514a36-538e-43b7-83b7-ee9187c16655")
        String fileId,
        @Schema(description = "에디터 <img> src 바인딩용 조회 URL (Froala 등 표준 link 필드)", example = "/feature/file-handler/view/95514a36-538e-43b7-83b7-ee9187c16655")
        String link
) {
    //
    public static TempFileRegisteredResponse of(String fileId) {
        //
        return new TempFileRegisteredResponse(fileId, "/feature/file-handler/view/" + fileId);
    }
}

